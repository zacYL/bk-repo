/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
 *
 * Copyright (C) 2021 THL A29 Limited, a Tencent company.  All rights reserved.
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

package com.tencent.bkrepo.common.metadata.service.metadata.impl

import com.tencent.bkrepo.common.api.constant.StringPool
import com.tencent.bkrepo.common.api.exception.ErrorCodeException
import com.tencent.bkrepo.common.api.message.CommonMessageCode
import com.tencent.bkrepo.common.artifact.constant.CUSTOM
import com.tencent.bkrepo.common.artifact.constant.LOCK_STATUS
import com.tencent.bkrepo.common.artifact.constant.PIPELINE
import com.tencent.bkrepo.common.artifact.constant.RESERVED_KEY
import com.tencent.bkrepo.common.artifact.exception.NodeNotFoundException
import com.tencent.bkrepo.common.artifact.message.ArtifactMessageCode
import com.tencent.bkrepo.common.artifact.path.PathUtils
import com.tencent.bkrepo.common.artifact.path.PathUtils.normalizeFullPath
import com.tencent.bkrepo.common.metadata.condition.SyncCondition
import com.tencent.bkrepo.common.metadata.config.RepositoryProperties
import com.tencent.bkrepo.common.metadata.dao.node.NodeDao
import com.tencent.bkrepo.common.metadata.model.TMetadata
import com.tencent.bkrepo.common.metadata.model.TNode
import com.tencent.bkrepo.common.metadata.service.metadata.MetadataService
import com.tencent.bkrepo.common.metadata.service.node.impl.NodeBaseService
import com.tencent.bkrepo.common.metadata.util.ClusterUtils
import com.tencent.bkrepo.common.metadata.util.MetadataUtils
import com.tencent.bkrepo.common.metadata.util.NodeEventFactory.buildMetadataDeletedEvent
import com.tencent.bkrepo.common.metadata.util.NodeEventFactory.buildMetadataSavedEvent
import com.tencent.bkrepo.common.metadata.util.NodeQueryHelper
import com.tencent.bkrepo.common.security.exception.PermissionException
import com.tencent.bkrepo.common.security.manager.ci.CIPermissionManager
import com.tencent.bkrepo.common.service.cluster.condition.DefaultCondition
import com.tencent.bkrepo.common.service.util.SpringContextUtils.Companion.publishEvent
import com.tencent.bkrepo.repository.constant.SYSTEM_USER
import com.tencent.bkrepo.repository.message.RepositoryMessageCode
import com.tencent.bkrepo.repository.pojo.metadata.LimitType
import com.tencent.bkrepo.repository.pojo.metadata.MetadataDeleteRequest
import com.tencent.bkrepo.repository.pojo.metadata.MetadataSaveRequest
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Conditional
import org.springframework.data.mongodb.core.query.Query
import org.springframework.data.mongodb.core.query.Update
import org.springframework.data.mongodb.core.query.inValues
import org.springframework.data.mongodb.core.query.where
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

/**
 * 元数据服务实现类
 */
@Service
@Conditional(SyncCondition::class, DefaultCondition::class)
class MetadataServiceImpl(
    private val nodeDao: NodeDao,
    private val nodeBaseService: NodeBaseService,
    private val repositoryProperties: RepositoryProperties,
    private val ciPermissionManager: CIPermissionManager
) : MetadataService {

    override fun listMetadata(projectId: String, repoName: String, fullPath: String): Map<String, Any> {
        return MetadataUtils.toMap(nodeDao.findOne(NodeQueryHelper.nodeQuery(projectId, repoName, fullPath))?.metadata)
    }

    @Transactional(rollbackFor = [Throwable::class])
    override fun saveMetadata(request: MetadataSaveRequest) {
        with(request) {
            if (metadata.isNullOrEmpty() && nodeMetadata.isNullOrEmpty()) {
                logger.info("Metadata is empty, skip saving")
                return
            }
            val fullPath = normalizeFullPath(fullPath)
            val node = nodeDao.findNode(projectId, repoName, fullPath)
                ?: throw ErrorCodeException(ArtifactMessageCode.NODE_NOT_FOUND, fullPath)
            checkNodeCluster(node)
            val systemMetadata = nodeMetadata?.filter { it.key in RESERVED_KEY }
            if (systemMetadata.isNullOrEmpty() &&
                (node.metadata?.any { it.key == LOCK_STATUS && it.value == true } == true)
            ) {
                throw ErrorCodeException(ArtifactMessageCode.NODE_LOCK, node.fullPath)
            }
            val oldMetadata = node.metadata ?: ArrayList()
            val newMetadata = MetadataUtils.compatibleConvertAndCheck(
                metadata,
                MetadataUtils.changeSystem(nodeMetadata, repositoryProperties.allowUserAddSystemMetadata)
            )
            checkIfModifyPipelineMetadata(node, newMetadata.map { it.key })
            checkIfUpdateSystemMetadata(oldMetadata, newMetadata)
            MetadataUtils.checkEmptyAndLength(newMetadata)
            node.metadata = if (replace) {
                newMetadata
            } else {
                MetadataUtils.merge(oldMetadata, newMetadata)
            }

            if (newMetadata.all { it.key in RESERVED_KEY }) {
                nodeDao.save(node)
            } else {
                val currentTime = LocalDateTime.now()
                node.lastModifiedBy = operator
                node.lastModifiedDate = currentTime
                nodeDao.save(node)
                if (nodeBaseService.repositoryProperties.parentFolderUpdateEnabled) {
                    // 更新父目录的修改时间
                    val parentFullPath = PathUtils.toFullPath(PathUtils.resolveParent(fullPath))
                    nodeBaseService.updateModifiedInfo(projectId, repoName, parentFullPath, operator, currentTime)
                }
            }
            publishEvent(buildMetadataSavedEvent(request))
            logger.info("Save metadata[$newMetadata] on node[/$projectId/$repoName$fullPath] success.")
        }
    }

    @Transactional(rollbackFor = [Throwable::class])
    override fun addLimitMetadata(request: MetadataSaveRequest, limitType: LimitType) {
        with(request) {
            val limitMetadata = MetadataUtils.extractLimitMetadata(metadata = nodeMetadata!!, limitType = limitType)
            if (limitMetadata.isNullOrEmpty()) {
                logger.info("limitMetadata is empty, skip saving[$request]")
                return
            }
            saveMetadata(request.copy(metadata = null, nodeMetadata = limitMetadata, operator = SYSTEM_USER))
        }
    }

    @Transactional(rollbackFor = [Throwable::class])
    override fun deleteMetadata(request: MetadataDeleteRequest, allowDeleteSystemMetadata: Boolean) {
        with(request) {
            if (keyList.isEmpty()) {
                logger.info("Metadata key list is empty, skip deleting")
                return
            }
            val fullPath = normalizeFullPath(request.fullPath)
            val query = NodeQueryHelper.nodeQuery(projectId, repoName, fullPath)

            // 检查是否有更新权限
            val node = nodeDao.findOne(query) ?: throw NodeNotFoundException(fullPath)
            ClusterUtils.checkContainsSrcCluster(node.clusterNames)
            checkIfModifyPipelineMetadata(node, request.keyList)
            node.metadata?.forEach {
                if (it.key in keyList && it.system && !allowDeleteSystemMetadata) {
                    throw PermissionException("No permission to update system metadata[${it.key}]")
                }
                when {
                    it.key == LOCK_STATUS && it.value == true -> {
                        throw ErrorCodeException(ArtifactMessageCode.NODE_LOCK, fullPath)
                    }
                    it.key in keyList -> MetadataUtils.checkPermission(it, operator)
                }
            }

            val currentTime = LocalDateTime.now()
            val update = Update().pull(
                TNode::metadata.name,
                Query.query(where(TMetadata::key).inValues(keyList))
            ).set(TNode::lastModifiedDate.name, currentTime).set(TNode::lastModifiedBy.name, operator)
            nodeDao.updateMulti(query, update)
            if (nodeBaseService.repositoryProperties.parentFolderUpdateEnabled) {
                // 更新父目录的修改时间
                val parentFullPath = PathUtils.toFullPath(PathUtils.resolveParent(fullPath))
                nodeBaseService.updateModifiedInfo(projectId, repoName, parentFullPath, operator, currentTime)
            }
            publishEvent(buildMetadataDeletedEvent(this))
            logger.info("Delete metadata[$keyList] on node[/$projectId/$repoName$fullPath] success.")
        }
    }

    fun checkNodeCluster(node: TNode) {
        return
    }

    private fun checkIfModifyPipelineMetadata(node: TNode, newMetadataKeys: Collection<String>) {
        val pipelineSource = node.repoName == PIPELINE || node.repoName == CUSTOM
        val pipelineMetadataKey = newMetadataKeys.find {
            CIPermissionManager.PIPELINE_METADATA.any { m -> m.equals(it, true) }
        }
        val illegal = !node.folder && pipelineSource &&
            pipelineMetadataKey != null && !ciPermissionManager.whiteListRequest()
        if (illegal) {
            ciPermissionManager.throwOrLogError(
                messageCode = RepositoryMessageCode.PIPELINE_METADATA_UPDATE_NOT_ALLOWED,
                pipelineMetadataKey!!
            )
        }
    }

    /**
     * 检查是否有更新允许用户添加的系统元数据
     */
    private fun checkIfUpdateSystemMetadata(
        oldMetadata: MutableList<TMetadata>,
        newMetadata: MutableList<TMetadata>
    ) {
        val oldAllowUserAddSystemMetadata =
            oldMetadata.map { it.key }.intersectIgnoreCase(repositoryProperties.allowUserAddSystemMetadata)
        val newAllowUserAddSystemMetadata =
            newMetadata.map { it.key }.intersectIgnoreCase(repositoryProperties.allowUserAddSystemMetadata)
        val updateSystemMetadata = oldAllowUserAddSystemMetadata.intersect(newAllowUserAddSystemMetadata)
        if (updateSystemMetadata.isNotEmpty()) {
            throw ErrorCodeException(
                CommonMessageCode.PARAMETER_INVALID,
                updateSystemMetadata.joinToString(StringPool.COMMA)
            )
        }
    }

    private fun List<String>.intersectIgnoreCase(list: List<String>): List<String> {
        return this.filter { k -> list.any { it.equals(k, true) } }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(MetadataServiceImpl::class.java)
    }
}
