package com.tencent.bkrepo.repository.cpack.service.impl

import com.tencent.bkrepo.auth.pojo.enums.PermissionAction
import com.tencent.bkrepo.common.api.exception.ErrorCodeException
import com.tencent.bkrepo.common.api.message.CommonMessageCode
import com.tencent.bkrepo.common.api.util.toJsonString
import com.tencent.bkrepo.common.artifact.constant.LOCK_STATUS
import com.tencent.bkrepo.common.artifact.message.ArtifactMessageCode
import com.tencent.bkrepo.common.artifact.path.PathUtils
import com.tencent.bkrepo.common.metadata.dao.node.NodeDao
import com.tencent.bkrepo.common.metadata.model.TNode
import com.tencent.bkrepo.common.metadata.permission.PermissionManager
import com.tencent.bkrepo.common.metadata.service.node.impl.NodeBaseService
import com.tencent.bkrepo.common.metadata.service.repo.QuotaService
import com.tencent.bkrepo.common.metadata.util.MetadataUtils
import com.tencent.bkrepo.common.metadata.util.NodeEventFactory
import com.tencent.bkrepo.common.metadata.util.NodeQueryHelper
import com.tencent.bkrepo.common.service.util.SpringContextUtils.Companion.publishEvent
import com.tencent.bkrepo.repository.cpack.pojo.node.service.NodeBatchDeleteRequest
import com.tencent.bkrepo.repository.cpack.service.NodeCpackService
import com.tencent.bkrepo.repository.pojo.node.UserAuthPathOption
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.dao.DuplicateKeyException
import org.springframework.data.mongodb.core.query.Criteria
import org.springframework.data.mongodb.core.query.Query
import org.springframework.data.mongodb.core.query.Update
import org.springframework.data.mongodb.core.query.and
import org.springframework.data.mongodb.core.query.inValues
import org.springframework.data.mongodb.core.query.isEqualTo
import org.springframework.data.mongodb.core.query.where
import org.springframework.stereotype.Service
import java.time.LocalDateTime

@Service
class NodeCpackServiceImpl(
    private val nodeBaseService: NodeBaseService,
    private val quotaService: QuotaService,
    private val nodeDao: NodeDao,
    private val permissionManager: PermissionManager
) : NodeCpackService {
    override fun nodeBatchDelete(nodeBatchDeleteRequest: NodeBatchDeleteRequest) {
        val userAuthPath = permissionManager.getUserAuthPathCache(
            UserAuthPathOption(
                nodeBatchDeleteRequest.operator,
                nodeBatchDeleteRequest.projectId,
                listOf(nodeBatchDeleteRequest.repoName),
                PermissionAction.DELETE
            )
        )[nodeBatchDeleteRequest.repoName]?: emptyList()
        val existNodes = existNodes(nodeBatchDeleteRequest)
        // 删除文件夹
        with(nodeBatchDeleteRequest) {
            if (existNodes.isEmpty()||userAuthPath.isEmpty()) return

            val authOrOperation = mutableListOf(
                where(TNode::fullPath).inValues(userAuthPath)
            )

            userAuthPath.forEach {
                val normalizedFullPath = PathUtils.normalizeFullPath(it)
                val normalizedPath = PathUtils.toPath(normalizedFullPath)
                val escapedPath = PathUtils.escapeRegex(normalizedPath)
                authOrOperation.apply {
                    add(where(TNode::fullPath).regex("^$escapedPath"))
                }
            }

            val orOperation = mutableListOf(
                where(TNode::fullPath).inValues(existNodes)
            )
            existNodes.forEach {
                val normalizedPath = PathUtils.toPath(it)
                val escapedPath = PathUtils.escapeRegex(normalizedPath)
                orOperation.apply {
                    add(where(TNode::fullPath).regex("^$escapedPath"))
                }
            }


            val authOrCriteria = Criteria().orOperator(*authOrOperation.toTypedArray())

            val orCriteria = Criteria().orOperator(*orOperation.toTypedArray())

            val andOperator = Criteria().andOperator(authOrCriteria, orCriteria)

            val criteria = where(TNode::projectId).isEqualTo(projectId)
                .and(TNode::repoName).isEqualTo(repoName)
                .and(TNode::deleted).isEqualTo(null)
                .andOperator(andOperator)
            val query = Query(criteria)
            val deleteNodesSize = nodeBaseService.aggregateComputeSize(criteria)
            try {
                quotaService.decreaseUsedVolume(projectId, repoName, deleteNodesSize)
                val now = LocalDateTime.now()
                nodeDao.updateMulti(query, NodeQueryHelper.nodeDeleteUpdate(operator, now))
                // show in recycle bin
                nodeDao.updateMulti(
                    NodeQueryHelper.nodeQuery(projectId, repoName, existNodes, now),
                    Update().push(TNode::metadata.name, MetadataUtils.buildRecycleBinMetadata())
                )
                // 批量更新上层目录的修改信息
                val parentFullPaths = existNodes
                    .map { PathUtils.toFullPath(PathUtils.resolveParent(it)) }
                    .distinct()
                    .filterNot { PathUtils.isRoot(it) }
                val parentNodeQuery = NodeQueryHelper.nodeQuery(projectId, repoName, parentFullPaths)
                val parentNodeUpdate = NodeQueryHelper.update(operator)
                nodeDao.updateMulti(parentNodeQuery, parentNodeUpdate)
                NodeEventFactory
                    .buildBatchDeletedEvent(projectId, repoName, existNodes, operator)
                    .forEach { publishEvent(it) }
            } catch (exception: DuplicateKeyException) {
                logger.warn(
                    " Delete node[/$projectId/$repoName${existNodes.toJsonString()}] " +
                        "by [$operator] error: [${exception.message}]"
                )
            }
            logger.info("Delete node[/$projectId/$repoName${existNodes.toJsonString()}] by [$operator] success.")
        }
    }

    private fun existNodes(nodeBatchDeleteRequest: NodeBatchDeleteRequest): List<String> {
        nodeBatchDeleteRequest.fullPaths.forEach {
            // 不允许直接删除根目录
            if (PathUtils.isRoot(it)) {
                throw ErrorCodeException(CommonMessageCode.METHOD_NOT_ALLOWED, "Can't delete root node.")
            }
        }
        with(nodeBatchDeleteRequest) {
            val checkCriteria = where(TNode::projectId).isEqualTo(projectId)
                .and(TNode::repoName).isEqualTo(repoName)
                .and(TNode::deleted).isEqualTo(null)
                .and(TNode::fullPath).inValues(fullPaths)
            val checkQuery = Query(checkCriteria)
            val node=nodeDao.find(checkQuery)
            if (node.any { it.metadata?.any { it.key == LOCK_STATUS && it.value == true } == true }) {
                val errorCode = if (node.size == 1) ArtifactMessageCode.NODE_LOCK else ArtifactMessageCode.NODE_CHILD_LOCK
                throw ErrorCodeException(errorCode,fullPaths)
            }
            return node.map { it.fullPath }
        }
    }

    /**
     * 统计批量删除文件数量
     */
    override fun countBatchDeleteNodes(nodeBatchDeleteRequest: NodeBatchDeleteRequest): Long {
        // 不允许直接删除根目录
        val userAuthPath = permissionManager.getUserAuthPathCache(
            UserAuthPathOption(
                nodeBatchDeleteRequest.operator,
                nodeBatchDeleteRequest.projectId,
                listOf(nodeBatchDeleteRequest.repoName),
                PermissionAction.DELETE
            )
        )[nodeBatchDeleteRequest.repoName]?: emptyList()
        val existNodes = existNodes(nodeBatchDeleteRequest)

        // 统计文件夹下制品数
        with(nodeBatchDeleteRequest) {
            if (existNodes.isEmpty()||userAuthPath.isEmpty()) return 0L

            val authOrOperation = mutableListOf(
                where(TNode::fullPath).inValues(userAuthPath)
            )

            userAuthPath.forEach {
                val normalizedFullPath = PathUtils.normalizeFullPath(it)
                val normalizedPath = PathUtils.toPath(normalizedFullPath)
                val escapedPath = PathUtils.escapeRegex(normalizedPath)
                authOrOperation.apply {
                    add(where(TNode::fullPath).regex("^$escapedPath"))
                }
            }

            val orOperation = mutableListOf(
                where(TNode::fullPath).inValues(existNodes)
            )
            existNodes.forEach {
                val normalizedFullPath = PathUtils.normalizeFullPath(it)
                val normalizedPath = PathUtils.toPath(normalizedFullPath)
                val escapedPath = PathUtils.escapeRegex(normalizedPath)
                orOperation.apply {
                    add(where(TNode::fullPath).regex("^$escapedPath"))
                }
            }

            val authOrCriteria = Criteria().orOperator(*authOrOperation.toTypedArray())

            val orCriteria = Criteria().orOperator(*orOperation.toTypedArray())

            val andOperator = Criteria().andOperator(authOrCriteria, orCriteria)

            val criteria = where(TNode::projectId).isEqualTo(projectId)
                .and(TNode::repoName).isEqualTo(repoName)
                .and(TNode::deleted).isEqualTo(null)
                .and(TNode::folder).isEqualTo(false)
                .andOperator(andOperator)
            return nodeDao.count(Query.query(criteria))
        }
    }

    companion object {
        private val logger: Logger = LoggerFactory.getLogger(NodeCpackServiceImpl::class.java)
    }
}
