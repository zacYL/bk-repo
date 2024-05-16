/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
 *
 * Copyright (C) 2022 THL A29 Limited, a Tencent company.  All rights reserved.
 *
 * BK-CI 蓝鲸持续集成平台 is licensed under the MIT license.
 *
 * A copy of the MIT License is included in this file.
 *
 *
 * Terms of the MIT License:
 * ---------------------------------------------------
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
 * documentation files (the "Software"), to deal in the Software without restriction, including without limitation the
 * rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to
 * permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of
 * the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT
 * LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN
 * NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
 * SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package com.tencent.bkrepo.conan.service.impl

import com.tencent.bkrepo.common.artifact.api.ArtifactFile
import com.tencent.bkrepo.common.artifact.repository.context.ArtifactContextHolder
import com.tencent.bkrepo.common.artifact.repository.context.ArtifactDownloadContext
import com.tencent.bkrepo.common.artifact.repository.context.ArtifactUploadContext
import com.tencent.bkrepo.common.artifact.resolve.response.ArtifactChannel
import com.tencent.bkrepo.common.service.util.SpringContextUtils
import com.tencent.bkrepo.conan.constant.EXPORT_SOURCES_TGZ_NAME
import com.tencent.bkrepo.conan.constant.PACKAGE_TGZ_NAME
import com.tencent.bkrepo.conan.listener.event.ConanPackageUploadEvent
import com.tencent.bkrepo.conan.listener.event.ConanRecipeUploadEvent
import com.tencent.bkrepo.conan.pojo.artifact.ConanArtifactInfo
import com.tencent.bkrepo.conan.service.ConanUploadDownloadService
import com.tencent.bkrepo.conan.utils.ObjectBuildUtil
import com.tencent.bkrepo.conan.utils.PathUtils.generateFullPath
import com.tencent.bkrepo.repository.api.PackageClient
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

/**
 * conan文件上传 下载
 */
@Service
class ConanUploadDownloadServiceImpl(
    private val packageClient: PackageClient,
) : ConanUploadDownloadService {

    @Autowired
    lateinit var commonService: CommonService

    override fun uploadFile(conanArtifactInfo: ConanArtifactInfo, artifactFile: ArtifactFile) {
        if (artifactFile.getSize() != 0L) {
            val context = ArtifactUploadContext(artifactFile)
            ArtifactContextHolder.getRepository().upload(context)
        } else {
            // conan客户端上传文件前会使用同样请求去确认文件是否存在
            val fullPath = generateFullPath(conanArtifactInfo)
            commonService.checkNodeExist(conanArtifactInfo.projectId, conanArtifactInfo.repoName, fullPath)
        }
    }

    override fun downloadFile(conanArtifactInfo: ConanArtifactInfo) {
        val context = ArtifactDownloadContext()
        ArtifactContextHolder.getRepository().download(context)
    }

    override fun handleConanArtifactUpload(userId: String, artifactInfo: ConanArtifactInfo) {
        val fullPath = generateFullPath(artifactInfo)
        if (fullPath.endsWith(EXPORT_SOURCES_TGZ_NAME)) {
            // TODO package version size 如何计算
            createVersion(
                artifactInfo = artifactInfo,
                userId = userId,
                size = 0
            )
            SpringContextUtils.publishEvent(
                ConanRecipeUploadEvent(
                    ObjectBuildUtil.buildConanRecipeUpload(artifactInfo, userId)
                )
            )
        }
        if (fullPath.endsWith(PACKAGE_TGZ_NAME)) {
            SpringContextUtils.publishEvent(
                ConanPackageUploadEvent(
                    ObjectBuildUtil.buildConanPackageUpload(artifactInfo, userId)
                )
            )
        }
    }

    /**
     * 创建包版本
     */
    fun createVersion(
        userId: String,
        artifactInfo: ConanArtifactInfo,
        size: Long,
        sourceType: ArtifactChannel? = null,
    ) {
        val packageVersionCreateRequest = ObjectBuildUtil.buildPackageVersionCreateRequest(
            userId = userId,
            artifactInfo = artifactInfo,
            size = size,
            sourceType = sourceType
        )
        // TODO 元数据中要加入对应username与channel，可能存在同一制品版本存在不同username与channel
        val packageUpdateRequest = ObjectBuildUtil.buildPackageUpdateRequest(artifactInfo)
        packageClient.createVersion(packageVersionCreateRequest).apply {
            logger.info("user: [$userId] create package version [$packageVersionCreateRequest] success!")
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(this::class.java)
    }
}
