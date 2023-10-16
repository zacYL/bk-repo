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

package com.tencent.bkrepo.repository.service.metadata.impl

import com.tencent.bkrepo.common.api.exception.ErrorCodeException
import com.tencent.bkrepo.common.artifact.constant.LOCK_STATUS
import com.tencent.bkrepo.common.artifact.constant.RESERVED_KEY
import com.tencent.bkrepo.common.artifact.message.ArtifactMessageCode
import com.tencent.bkrepo.common.artifact.path.PathUtils
import com.tencent.bkrepo.common.artifact.path.PathUtils.normalizeFullPath
import com.tencent.bkrepo.common.service.util.SpringContextUtils.Companion.publishEvent
import com.tencent.bkrepo.repository.constant.SYSTEM_USER
import com.tencent.bkrepo.repository.dao.NodeDao
import com.tencent.bkrepo.repository.model.TMetadata
import com.tencent.bkrepo.repository.model.TNode
import com.tencent.bkrepo.repository.pojo.metadata.LimitType
import com.tencent.bkrepo.repository.pojo.metadata.MetadataDeleteRequest
import com.tencent.bkrepo.repository.pojo.metadata.MetadataSaveRequest
import com.tencent.bkrepo.repository.service.metadata.MetadataService
import com.tencent.bkrepo.repository.service.node.impl.NodeBaseService
import com.tencent.bkrepo.repository.util.MetadataUtils
import com.tencent.bkrepo.repository.util.NodeEventFactory.buildMetadataDeletedEvent
import com.tencent.bkrepo.repository.util.NodeEventFactory.buildMetadataSavedEvent
import com.tencent.bkrepo.repository.util.NodeQueryHelper
import org.slf4j.LoggerFactory
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
class MetadataServiceImpl(
    private val nodeDao: NodeDao,
    private val nodeBaseService: NodeBaseService
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
            val systemMetadata=nodeMetadata?.filter { it.key in RESERVED_KEY }
            if (systemMetadata.isNullOrEmpty() && (node.metadata?.any { it.key == LOCK_STATUS && it.value == true } == true)) {
                throw ErrorCodeException(ArtifactMessageCode.NODE_LOCK, node.fullPath)
            }
            val oldMetadata = node.metadata ?: ArrayList()
            val newMetadata = MetadataUtils.compatibleFromAndCheck(metadata, nodeMetadata, operator)
            MetadataUtils.checkEmptyAndLength(newMetadata)
            node.metadata = MetadataUtils.checkAndMerge(oldMetadata, newMetadata, operator)

            val currentTime = LocalDateTime.now()
            node.lastModifiedBy = operator
            node.lastModifiedDate = currentTime
            nodeDao.save(node)
            // 更新父目录的修改时间
            val parentFullPath = PathUtils.toFullPath(PathUtils.resolveParent(fullPath))
            nodeBaseService.updateModifiedInfo(projectId, repoName, parentFullPath, operator, currentTime)
            publishEvent(buildMetadataSavedEvent(request))
            logger.info("Save metadata[$metadata] on node[/$projectId/$repoName$fullPath] success.")
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
    override fun deleteMetadata(request: MetadataDeleteRequest) {
        with(request) {
            if (keyList.isEmpty()) {
                logger.info("Metadata key list is empty, skip deleting")
                return
            }
            val fullPath = normalizeFullPath(request.fullPath)
            val query = NodeQueryHelper.nodeQuery(projectId, repoName, fullPath)

            // 检查是否有更新权限
            nodeDao.findOne(query)?.metadata?.forEach {
                when {
                    it.key == LOCK_STATUS && it.value == true -> throw ErrorCodeException(ArtifactMessageCode.NODE_LOCK, fullPath)
                    it.key in keyList -> MetadataUtils.checkPermission(it, operator)
                }
            }

            val currentTime = LocalDateTime.now()
            val update = Update().pull(
                TNode::metadata.name,
                Query.query(where(TMetadata::key).inValues(keyList))
            ).set(TNode::lastModifiedDate.name, currentTime).set(TNode::lastModifiedBy.name, operator)
            nodeDao.updateMulti(query, update)
            // 更新父目录的修改时间
            val parentFullPath = PathUtils.toFullPath(PathUtils.resolveParent(fullPath))
            nodeBaseService.updateModifiedInfo(projectId, repoName, parentFullPath, operator, currentTime)
            publishEvent(buildMetadataDeletedEvent(this))
            logger.info("Delete metadata[$keyList] on node[/$projectId/$repoName$fullPath] success.")
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(MetadataServiceImpl::class.java)
    }
}
