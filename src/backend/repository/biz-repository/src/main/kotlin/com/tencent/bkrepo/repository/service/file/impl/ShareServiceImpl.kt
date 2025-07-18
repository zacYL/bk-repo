/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
 *
 * Copyright (C) 2021 Tencent.  All rights reserved.
 *
 * BK-CI 蓝鲸持续集成平台 is licensed under the MIT license.
 *
 * A copy of the MIT License is included in this file.
 *
 *
 * Terms of the MIT License:
 * ---------------------------------------------------
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package com.tencent.bkrepo.repository.service.file.impl

import com.tencent.bk.audit.context.ActionAuditContext
import com.tencent.bkrepo.auth.api.ServiceTemporaryTokenClient
import com.tencent.bkrepo.auth.pojo.token.TemporaryTokenCreateRequest
import com.tencent.bkrepo.auth.pojo.token.TokenType
import com.tencent.bkrepo.common.api.constant.ANONYMOUS_USER
import com.tencent.bkrepo.common.api.constant.AUDITED_UID
import com.tencent.bkrepo.common.api.constant.AUDIT_REQUEST_URI
import com.tencent.bkrepo.common.api.constant.AUDIT_SHARE_USER_ID
import com.tencent.bkrepo.common.api.constant.USER_KEY
import com.tencent.bkrepo.common.api.exception.ErrorCodeException
import com.tencent.bkrepo.common.artifact.api.ArtifactInfo
import com.tencent.bkrepo.common.artifact.exception.NodeNotFoundException
import com.tencent.bkrepo.common.artifact.message.ArtifactMessageCode
import com.tencent.bkrepo.common.artifact.repository.context.ArtifactContextHolder
import com.tencent.bkrepo.common.artifact.repository.context.ArtifactDownloadContext
import com.tencent.bkrepo.common.metadata.service.node.NodeService
import com.tencent.bkrepo.common.metadata.service.repo.RepositoryService
import com.tencent.bkrepo.common.security.exception.PermissionException
import com.tencent.bkrepo.common.service.cluster.condition.DefaultCondition
import com.tencent.bkrepo.common.service.util.HttpContextHolder
import com.tencent.bkrepo.repository.model.TShareRecord
import com.tencent.bkrepo.repository.pojo.share.ShareRecordCreateRequest
import com.tencent.bkrepo.repository.pojo.share.ShareRecordInfo
import com.tencent.bkrepo.repository.service.file.ShareService
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Conditional
import org.springframework.data.mongodb.core.MongoTemplate
import org.springframework.data.mongodb.core.query.Criteria
import org.springframework.data.mongodb.core.query.Query
import org.springframework.data.mongodb.core.query.and
import org.springframework.data.mongodb.core.query.isEqualTo
import org.springframework.data.mongodb.core.query.where
import org.springframework.stereotype.Service
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/**
 * 文件分享服务实现类
 */
@Service
@Conditional(DefaultCondition::class)
class ShareServiceImpl(
    private val repositoryService: RepositoryService,
    private val nodeService: NodeService,
    private val mongoTemplate: MongoTemplate,
    private val temporaryTokenClient: ServiceTemporaryTokenClient,
) : ShareService {

    override fun create(
        userId: String,
        artifactInfo: ArtifactInfo,
        request: ShareRecordCreateRequest,
    ): ShareRecordInfo {
        with(artifactInfo) {
            checkNode(artifactInfo)
            // 兼容性代码，把token的创建统一迁移到temporary_token 表
            val tmpToken = TemporaryTokenCreateRequest(
                projectId = projectId,
                repoName = repoName,
                fullPathSet = setOf(getArtifactFullPath()),
                authorizedUserSet = request.authorizedUserList.toSet(),
                authorizedIpSet = request.authorizedIpList.toSet(),
                createdBy = userId,
                type = TokenType.DOWNLOAD,
                expireSeconds = request.expireSeconds,
            )
            val token = temporaryTokenClient.createToken(tmpToken).data!!.first().token
            val shareRecord = TShareRecord(
                projectId = projectId,
                repoName = repoName,
                fullPath = getArtifactFullPath(),
                expireDate = computeExpireDate(request.expireSeconds),
                authorizedUserList = request.authorizedUserList,
                authorizedIpList = request.authorizedIpList,
                token = token,
                createdBy = userId,
                createdDate = LocalDateTime.now(),
                lastModifiedBy = userId,
                lastModifiedDate = LocalDateTime.now(),
            )
            mongoTemplate.save(shareRecord)
            val shareRecordInfo = convert(shareRecord)
            logger.info("$userId create share record[$shareRecordInfo] success.")
            return shareRecordInfo
        }
    }

    override fun checkToken(userId: String, token: String, artifactInfo: ArtifactInfo): ShareRecordInfo {
        with(artifactInfo) {
            val query = Query.query(
                where(TShareRecord::projectId).isEqualTo(artifactInfo.projectId)
                    .and(TShareRecord::repoName).isEqualTo(repoName)
                    .and(TShareRecord::fullPath).isEqualTo(getArtifactFullPath())
                    .and(TShareRecord::token).isEqualTo(token),
            )
            val shareRecord = mongoTemplate.findOne(query, TShareRecord::class.java)
                ?: throw ErrorCodeException(ArtifactMessageCode.TEMPORARY_TOKEN_INVALID, token)
            if (shareRecord.authorizedUserList.isNotEmpty() && userId !in shareRecord.authorizedUserList) {
                throw PermissionException("unauthorized")
            }
            if (shareRecord.expireDate?.isBefore(LocalDateTime.now()) == true) {
                throw ErrorCodeException(ArtifactMessageCode.TEMPORARY_TOKEN_EXPIRED)
            }
            return convert(shareRecord)
        }
    }

    override fun download(userId: String, token: String, artifactInfo: ArtifactInfo) {
        logger.info("artifact[$artifactInfo] download user: $userId")
        val shareRecord = checkToken(userId, token, artifactInfo)
        checkAlphaApkDownloadUser(userId, artifactInfo, shareRecord.createdBy)
        with(artifactInfo) {
            val downloadUser = if (userId == ANONYMOUS_USER) shareRecord.createdBy else userId
            val repo = repositoryService.getRepoDetail(projectId, repoName)
                ?: throw ErrorCodeException(ArtifactMessageCode.REPOSITORY_NOT_FOUND, repoName)
            val context = ArtifactDownloadContext(repo = repo, userId = userId)
            HttpContextHolder.getRequest().setAttribute(USER_KEY, downloadUser)
            ActionAuditContext.current().addExtendData(AUDITED_UID, downloadUser)
            ActionAuditContext.current().addExtendData(
                AUDIT_REQUEST_URI, "{${HttpContextHolder.getRequestOrNull()?.requestURI}}"
            )
            ActionAuditContext.current().addExtendData(AUDIT_SHARE_USER_ID, shareRecord.createdBy)
            context.shareUserId = shareRecord.createdBy
            val repository = ArtifactContextHolder.getRepository(context.repositoryDetail.category)
            repository.download(context)
        }
    }

    override fun list(projectId: String, repoName: String, fullPath: String): List<ShareRecordInfo> {
        val query = Query.query(
            Criteria.where(TShareRecord::projectId.name).`is`(projectId)
                .and(TShareRecord::repoName.name).`is`(repoName)
                .and(TShareRecord::fullPath.name).`is`(fullPath),
        )
        return mongoTemplate.find(query, TShareRecord::class.java).map { convert(it) }
    }

    fun checkNode(artifactInfo: ArtifactInfo) {
        val node = nodeService.getNodeDetail(artifactInfo)
        if (node == null || node.folder) {
            throw NodeNotFoundException(artifactInfo.getArtifactFullPath())
        }
    }

    /**
     * 加固签名的apk包，匿名下载时，使用分享人身份下载
     */
    private fun checkAlphaApkDownloadUser(userId: String, artifactInfo: ArtifactInfo, shareUserId: String) {
        val nodeDetail = ArtifactContextHolder.getNodeDetail(artifactInfo)
            ?: throw NodeNotFoundException(artifactInfo.getArtifactFullPath())
        val appStageKey = nodeDetail.metadata.keys.find { it.equals(BK_CI_APP_STAGE_KEY, true) }
            ?: return
        val alphaApk = nodeDetail.metadata[appStageKey]?.toString().equals(ALPHA, true)
        if (alphaApk && userId == ANONYMOUS_USER) {
            HttpContextHolder.getRequest().setAttribute(USER_KEY, shareUserId)
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(ShareServiceImpl::class.java)
        private const val BK_CI_APP_STAGE_KEY = "BK-CI-APP-STAGE"
        private const val ALPHA = "Alpha"

        private fun generateShareUrl(shareRecord: TShareRecord): String {
            with(shareRecord) {
                return "/api/share/$projectId/$repoName$fullPath?token=$token"
            }
        }

        private fun computeExpireDate(expireSeconds: Long?): LocalDateTime? {
            return if (expireSeconds == null || expireSeconds <= 0) {
                null
            } else {
                LocalDateTime.now().plusSeconds(expireSeconds)
            }
        }

        private fun convert(tShareRecord: TShareRecord): ShareRecordInfo {
            return tShareRecord.let {
                ShareRecordInfo(
                    fullPath = it.fullPath,
                    repoName = it.repoName,
                    projectId = it.projectId,
                    shareUrl = generateShareUrl(it),
                    authorizedUserList = it.authorizedUserList,
                    authorizedIpList = it.authorizedIpList,
                    expireDate = it.expireDate?.format(DateTimeFormatter.ISO_DATE_TIME),
                    createdBy = it.createdBy,
                )
            }
        }
    }
}
