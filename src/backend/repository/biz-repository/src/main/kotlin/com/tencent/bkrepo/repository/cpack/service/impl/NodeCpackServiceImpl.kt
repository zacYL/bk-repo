package com.tencent.bkrepo.repository.cpack.service.impl

import com.tencent.bkrepo.common.api.exception.ErrorCodeException
import com.tencent.bkrepo.common.api.message.CommonMessageCode
import com.tencent.bkrepo.common.api.util.toJsonString
import com.tencent.bkrepo.common.artifact.path.PathUtils
import com.tencent.bkrepo.common.service.util.SpringContextUtils.Companion.publishEvent
import com.tencent.bkrepo.repository.cpack.pojo.node.service.NodeBatchDeleteRequest
import com.tencent.bkrepo.repository.cpack.service.NodeCpackService
import com.tencent.bkrepo.repository.dao.NodeDao
import com.tencent.bkrepo.repository.model.TNode
import com.tencent.bkrepo.repository.service.node.impl.NodeBaseService
import com.tencent.bkrepo.repository.service.repo.QuotaService
import com.tencent.bkrepo.repository.util.NodeEventFactory
import com.tencent.bkrepo.repository.util.NodeQueryHelper
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.dao.DuplicateKeyException
import org.springframework.data.mongodb.core.query.Criteria
import org.springframework.data.mongodb.core.query.Query
import org.springframework.data.mongodb.core.query.and
import org.springframework.data.mongodb.core.query.inValues
import org.springframework.data.mongodb.core.query.isEqualTo
import org.springframework.data.mongodb.core.query.where
import org.springframework.stereotype.Service

@Service
class NodeCpackServiceImpl(
    private val nodeBaseService: NodeBaseService,
    private val quotaService: QuotaService,
    private val nodeDao: NodeDao
) : NodeCpackService {
    override fun nodeBatchDelete(nodeBatchDeleteRequest: NodeBatchDeleteRequest) {
        val (nodes, paths) = reduceNode(nodeBatchDeleteRequest)
        batchDeleteNode(nodeBatchDeleteRequest, nodes)
        batchDeletePath(nodeBatchDeleteRequest, paths)
    }

    private fun reduceNode(nodeBatchDeleteRequest: NodeBatchDeleteRequest): Pair<List<String>, List<String>> {
        val nodes = mutableListOf<String>()
        val paths = mutableListOf<String>()
        nodeBatchDeleteRequest.fullPaths.forEach {
            // 不允许直接删除根目录 todo
            if (PathUtils.isRoot(it)) {
                throw ErrorCodeException(CommonMessageCode.METHOD_NOT_ALLOWED, "Can't delete root node.")
            }
            if (it.isNotBlank() && it.endsWith("/")) {
                paths.add(it)
            } else {
                nodes.add(it)
            }
        }
        with(nodeBatchDeleteRequest) {
            val checkCriteria = where(TNode::projectId).isEqualTo(projectId)
                .and(TNode::repoName).isEqualTo(repoName)
                .and(TNode::folder).isEqualTo(false)
                .and(TNode::deleted).isEqualTo(null)
                .and(TNode::fullPath).inValues(nodes)
            val checkQuery = Query(checkCriteria)
            val existNodes = nodeDao.find(checkQuery).map { it.fullPath }
            nodes.apply { this.clear(); addAll(existNodes) }
        }

        with(nodeBatchDeleteRequest) {
            val normalizedFullPaths = paths.map { PathUtils.normalizeFullPath(it) }
            val checkCriteria = where(TNode::projectId).isEqualTo(projectId)
                .and(TNode::repoName).isEqualTo(repoName)
                .and(TNode::folder).isEqualTo(true)
                .and(TNode::deleted).isEqualTo(null)
                .and(TNode::fullPath).inValues(normalizedFullPaths)
            val checkQuery = Query(checkCriteria)
            val existPath = nodeDao.find(checkQuery).map { it.fullPath }
            paths.apply { this.clear(); addAll(existPath) }
        }
        return Pair(nodes, paths)
    }

    /**
     * 删除多节点
     */
    private fun batchDeleteNode(nodeBatchDeleteRequest: NodeBatchDeleteRequest, nodes: List<String>) {
        // 删除节点
        with(nodeBatchDeleteRequest) {
            if (nodes.isEmpty()) return // 如果没有节点，则直接返回
            val criteria = where(TNode::projectId).isEqualTo(projectId)
                .and(TNode::repoName).isEqualTo(repoName)
                .and(TNode::deleted).isEqualTo(null)
                .and(TNode::fullPath).inValues(nodes)
            val query = Query(criteria)
            val deleteNodesSize = nodeBaseService.aggregateComputeSize(criteria)
            try {
                quotaService.decreaseUsedVolume(projectId, repoName, deleteNodesSize)
                nodeDao.updateMulti(query, NodeQueryHelper.nodeDeleteUpdate(operator))
                publishEvent(NodeEventFactory.buildBatchDeletedEvent(projectId, repoName, nodes, operator))
            } catch (exception: DuplicateKeyException) {
                logger.warn(
                    "Delete node[/$projectId/$repoName${nodes.toJsonString()}] " +
                        "by [$operator] error: [${exception.message}]"
                )
            }
            logger.info("Delete node[/$projectId/$repoName${nodes.toJsonString()}] by [$operator] success.")
        }
    }

    /**
     * 删除多文件夹
     */
    private fun batchDeletePath(nodeBatchDeleteRequest: NodeBatchDeleteRequest, paths: List<String>) {
        // 删除文件夹
        with(nodeBatchDeleteRequest) {
            if (paths.isEmpty()) return // 如果没有文件夹，则直接返回
            val orOperation = mutableListOf<Criteria>()
            paths.forEach {
                val normalizedPath = PathUtils.toPath(it)
                val escapedPath = PathUtils.escapeRegex(normalizedPath)
                orOperation.apply {
                    add(where(TNode::fullPath).regex("^$escapedPath"))
                    add(where(TNode::fullPath).isEqualTo(it))
                }
            }
            val criteria = where(TNode::projectId).isEqualTo(projectId)
                .and(TNode::repoName).isEqualTo(repoName)
                .and(TNode::deleted).isEqualTo(null)
                .orOperator(*orOperation.toTypedArray())
            val query = Query(criteria)
            val deleteNodesSize = nodeBaseService.aggregateComputeSize(criteria)
            try {
                quotaService.decreaseUsedVolume(projectId, repoName, deleteNodesSize)
                nodeDao.updateMulti(query, NodeQueryHelper.nodeDeleteUpdate(operator))
                publishEvent(NodeEventFactory.buildBatchDeletedEvent(projectId, repoName, paths, operator))
            } catch (exception: DuplicateKeyException) {
                logger.warn(
                    " Delete node[/$projectId/$repoName${paths.toJsonString()}] " +
                        "by [$operator] error: [${exception.message}]"
                )
            }
            logger.info("Delete node[/$projectId/$repoName${paths.toJsonString()}] by [$operator] success.")
        }
    }

    /**
     * 统计批量删除文件数量
     */
    override fun countBatchDeleteNodes(nodeBatchDeleteRequest: NodeBatchDeleteRequest): Long {
        // 不允许直接删除根目录
        val (nodes, paths) = reduceNode(nodeBatchDeleteRequest)
        // 统计文件夹下制品数
        with(nodeBatchDeleteRequest) {
            if (paths.isEmpty()) return 0L + nodes.size
            val orOperation = mutableListOf<Criteria>()
            paths.forEach {
                val normalizedFullPath = PathUtils.normalizeFullPath(it)
                val normalizedPath = PathUtils.toPath(normalizedFullPath)
                val escapedPath = PathUtils.escapeRegex(normalizedPath)
                orOperation.apply {
                    add(where(TNode::fullPath).regex("^$escapedPath"))
                }
            }
            val criteria = where(TNode::projectId).isEqualTo(projectId)
                .and(TNode::repoName).isEqualTo(repoName)
                .and(TNode::deleted).isEqualTo(null)
                .and(TNode::folder).isEqualTo(false)
                .orOperator(*orOperation.toTypedArray())
            return nodeDao.count(Query.query(criteria)) + nodes.size
        }
    }

    companion object {
        private val logger: Logger = LoggerFactory.getLogger(NodeCpackServiceImpl::class.java)
    }
}
