/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
 *
 * Copyright (C) 2020 THL A29 Limited, a Tencent company.  All rights reserved.
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

package com.tencent.bkrepo.repository.service.impl

import com.tencent.bkrepo.common.api.exception.ErrorCodeException
import com.tencent.bkrepo.common.api.message.CommonMessageCode
import com.tencent.bkrepo.common.api.pojo.Page
import com.tencent.bkrepo.common.api.util.Preconditions
import com.tencent.bkrepo.common.artifact.api.ArtifactInfo
import com.tencent.bkrepo.common.artifact.message.ArtifactMessageCode
import com.tencent.bkrepo.common.artifact.path.PathUtils.combineFullPath
import com.tencent.bkrepo.common.artifact.path.PathUtils.combinePath
import com.tencent.bkrepo.common.artifact.path.PathUtils.escapeRegex
import com.tencent.bkrepo.common.artifact.path.PathUtils.isRoot
import com.tencent.bkrepo.common.artifact.path.PathUtils.normalizeFullPath
import com.tencent.bkrepo.common.artifact.path.PathUtils.resolveName
import com.tencent.bkrepo.common.artifact.path.PathUtils.resolveParent
import com.tencent.bkrepo.common.artifact.path.PathUtils.toFullPath
import com.tencent.bkrepo.common.artifact.path.PathUtils.toPath
import com.tencent.bkrepo.common.artifact.pojo.RepositoryCategory
import com.tencent.bkrepo.common.mongo.dao.util.Pages
import com.tencent.bkrepo.common.storage.core.StorageService
import com.tencent.bkrepo.common.storage.credentials.StorageCredentials
import com.tencent.bkrepo.repository.config.RepositoryProperties
import com.tencent.bkrepo.repository.dao.NodeDao
import com.tencent.bkrepo.repository.dao.RepositoryDao
import com.tencent.bkrepo.repository.listener.event.node.NodeCopiedEvent
import com.tencent.bkrepo.repository.listener.event.node.NodeCreatedEvent
import com.tencent.bkrepo.repository.listener.event.node.NodeMovedEvent
import com.tencent.bkrepo.repository.listener.event.node.NodeRenamedEvent
import com.tencent.bkrepo.repository.listener.event.node.NodeUpdatedEvent
import com.tencent.bkrepo.repository.model.TNode
import com.tencent.bkrepo.repository.model.TRepository
import com.tencent.bkrepo.repository.pojo.node.CrossRepoNodeRequest
import com.tencent.bkrepo.repository.pojo.node.NodeDetail
import com.tencent.bkrepo.repository.pojo.node.NodeInfo
import com.tencent.bkrepo.repository.pojo.node.NodeListOption
import com.tencent.bkrepo.repository.pojo.node.NodeSizeInfo
import com.tencent.bkrepo.repository.pojo.node.service.NodeCopyRequest
import com.tencent.bkrepo.repository.pojo.node.service.NodeCreateRequest
import com.tencent.bkrepo.repository.pojo.node.service.NodeDeleteRequest
import com.tencent.bkrepo.repository.pojo.node.service.NodeMoveRequest
import com.tencent.bkrepo.repository.pojo.node.service.NodeRenameRequest
import com.tencent.bkrepo.repository.pojo.node.service.NodeUpdateRequest
import com.tencent.bkrepo.repository.service.FileReferenceService
import com.tencent.bkrepo.repository.service.NodeService
import com.tencent.bkrepo.repository.service.StorageCredentialService
import com.tencent.bkrepo.repository.util.MetadataUtils
import com.tencent.bkrepo.repository.util.NodeQueryHelper.nodeDeleteUpdate
import com.tencent.bkrepo.repository.util.NodeQueryHelper.nodeExpireDateUpdate
import com.tencent.bkrepo.repository.util.NodeQueryHelper.nodeListCriteria
import com.tencent.bkrepo.repository.util.NodeQueryHelper.nodeListQuery
import com.tencent.bkrepo.repository.util.NodeQueryHelper.nodePathUpdate
import com.tencent.bkrepo.repository.util.NodeQueryHelper.nodeQuery
import org.slf4j.LoggerFactory
import org.springframework.dao.DuplicateKeyException
import org.springframework.data.mongodb.core.aggregation.Aggregation
import org.springframework.data.mongodb.core.query.Criteria
import org.springframework.data.mongodb.core.query.Query
import org.springframework.data.mongodb.core.query.isEqualTo
import org.springframework.data.mongodb.core.query.where
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/**
 * 节点服务实现类
 */
@Service
class NodeServiceImpl(
    private val nodeDao: NodeDao,
    private val repositoryDao: RepositoryDao,
    private val fileReferenceService: FileReferenceService,
    private val storageCredentialService: StorageCredentialService,
    private val storageService: StorageService,
    private val repositoryProperties: RepositoryProperties
) : AbstractService(), NodeService {

    override fun getNodeDetail(artifact: ArtifactInfo, repoType: String?): NodeDetail? {
        with(artifact) {
            val node = nodeDao.findNode(projectId, repoName, getArtifactFullPath())
            return convertToDetail(node)
        }
    }

    override fun computeSize(artifact: ArtifactInfo): NodeSizeInfo {
        val projectId = artifact.projectId
        val repoName = artifact.repoName
        val fullPath = artifact.getArtifactFullPath()
        val node = nodeDao.findNode(projectId, repoName, fullPath)
            ?: throw ErrorCodeException(ArtifactMessageCode.NODE_NOT_FOUND, fullPath)
        // 节点为文件直接返回
        if (!node.folder) {
            return NodeSizeInfo(subNodeCount = 0, size = node.size)
        }
        val listOption = NodeListOption(includeFolder = true, deep = true)
        val criteria = nodeListCriteria(projectId, repoName, node.fullPath, listOption)
        val count = nodeDao.count(Query(criteria))
        val aggregation = Aggregation.newAggregation(
            Aggregation.match(criteria),
            Aggregation.group().sum(TNode::size.name).`as`(NodeSizeInfo::size.name)
        )
        val aggregateResult = nodeDao.aggregate(aggregation, HashMap::class.java)
        val size = if (aggregateResult.mappedResults.size > 0) {
            aggregateResult.mappedResults[0][NodeSizeInfo::size.name] as? Long ?: 0
        } else 0
        return NodeSizeInfo(subNodeCount = count, size = size)
    }

    override fun countFileNode(artifact: ArtifactInfo): Long {
        with(artifact) {
            val listOption = NodeListOption(
                includeFolder = false,
                includeMetadata = false,
                deep = true,
                sort = false
            )
            val query = nodeListQuery(projectId, repoName, getArtifactFullPath(), listOption)
            return nodeDao.count(query)
        }
    }

    override fun listNode(artifact: ArtifactInfo, option: NodeListOption): List<NodeInfo> {
        with(artifact) {
            val query = nodeListQuery(projectId, repoName, getArtifactFullPath(), option)
            if (nodeDao.count(query) > repositoryProperties.listCountLimit) {
                throw ErrorCodeException(ArtifactMessageCode.NODE_LIST_TOO_LARGE)
            }
            return nodeDao.find(query).map { convert(it)!! }
        }
    }

    override fun listNodePage(artifact: ArtifactInfo, option: NodeListOption): Page<NodeInfo> {
        with(artifact) {
            val pageNumber = option.pageNumber
            val pageSize = option.pageSize
            Preconditions.checkArgument(pageNumber >= 0, "pageNumber")
            Preconditions.checkArgument(pageSize >= 0 && pageSize <= repositoryProperties.listCountLimit, "pageSize")
            val query = nodeListQuery(projectId, repoName, getArtifactFullPath(), option)
            val totalRecords = nodeDao.count(query)
            val pageRequest = Pages.ofRequest(pageNumber, pageSize)
            val records = nodeDao.find(query.with(pageRequest)).map { convert(it)!! }

            return Pages.ofResponse(pageRequest, totalRecords, records)
        }
    }

    override fun checkExist(artifact: ArtifactInfo): Boolean {
        return nodeDao.exists(artifact.projectId, artifact.repoName, artifact.getArtifactFullPath())
    }

    override fun listExistFullPath(projectId: String, repoName: String, fullPathList: List<String>): List<String> {
        val queryList = fullPathList.map { normalizeFullPath(it) }.filter { !isRoot(it) }
        val nodeQuery = nodeQuery(projectId, repoName, queryList)
        return nodeDao.find(nodeQuery).map { it.fullPath }
    }

    @Transactional(rollbackFor = [Throwable::class])
    override fun createNode(createRequest: NodeCreateRequest): NodeDetail {
        with(createRequest) {
            val fullPath = normalizeFullPath(fullPath)
            Preconditions.checkArgument(!isRoot(fullPath), this::fullPath.name)
            Preconditions.checkArgument(folder || !sha256.isNullOrBlank(), this::sha256.name)
            Preconditions.checkArgument(folder || !md5.isNullOrBlank(), this::md5.name)
            // 路径唯一性校验
            nodeDao.findNode(projectId, repoName, fullPath)?.let {
                if (!overwrite) {
                    throw ErrorCodeException(ArtifactMessageCode.NODE_EXISTED, fullPath)
                } else if (it.folder || this.folder) {
                    throw ErrorCodeException(ArtifactMessageCode.NODE_CONFLICT, fullPath)
                } else {
                    deleteByPath(projectId, repoName, fullPath, operator)
                }
            }
            // 判断父目录是否存在，不存在先创建
            mkdirs(projectId, repoName, resolveParent(fullPath), operator)
            // 创建节点
            val node = TNode(
                projectId = projectId,
                repoName = repoName,
                path = resolveParent(fullPath),
                name = resolveName(fullPath),
                fullPath = fullPath,
                folder = folder,
                expireDate = if (folder) null else parseExpireDate(expires),
                size = if (folder) 0 else size ?: 0,
                sha256 = if (folder) null else sha256,
                md5 = if (folder) null else md5,
                metadata = MetadataUtils.fromMap(metadata),
                createdBy = createdBy ?: operator,
                createdDate = createdDate ?: LocalDateTime.now(),
                lastModifiedBy = createdBy ?: operator,
                lastModifiedDate = lastModifiedDate ?: LocalDateTime.now()
            )
            return node.apply { doCreate(this) }
                .also { publishEvent(NodeCreatedEvent(createRequest)) }
                .also { logger.info("Create node [$createRequest] success.") }
                .let { convertToDetail(it)!! }
        }
    }

    @Transactional(rollbackFor = [Throwable::class])
    override fun renameNode(renameRequest: NodeRenameRequest) {
        renameRequest.apply {
            val fullPath = normalizeFullPath(fullPath)
            val newFullPath = normalizeFullPath(newFullPath)
            val node = nodeDao.findNode(projectId, repoName, fullPath)
                ?: throw ErrorCodeException(ArtifactMessageCode.NODE_NOT_FOUND, fullPath)
            doRename(node, newFullPath, operator)
        }.also {
            publishEvent(NodeRenamedEvent(it))
        }.also {
            logger.info("Rename node [$it] success.")
        }
    }

    @Transactional(rollbackFor = [Throwable::class])
    override fun updateNode(updateRequest: NodeUpdateRequest) {
        updateRequest.apply {
            val fullPath = normalizeFullPath(fullPath)
            val node = nodeDao.findNode(projectId, repoName, fullPath)
                ?: throw ErrorCodeException(ArtifactMessageCode.NODE_NOT_FOUND, fullPath)
            val selfQuery = nodeQuery(projectId, repoName, node.fullPath)
            val selfUpdate = nodeExpireDateUpdate(parseExpireDate(expires), operator)
            nodeDao.updateFirst(selfQuery, selfUpdate)
        }.also {
            publishEvent(NodeUpdatedEvent(it))
        }.also {
            logger.info("Rename node [$it] success.")
        }
    }

    @Transactional(rollbackFor = [Throwable::class])
    override fun moveNode(moveRequest: NodeMoveRequest) {
        moveOrCopy(moveRequest, moveRequest.operator)
    }

    @Transactional(rollbackFor = [Throwable::class])
    override fun copyNode(copyRequest: NodeCopyRequest) {
        moveOrCopy(copyRequest, copyRequest.operator)
    }

    @Transactional(rollbackFor = [Throwable::class])
    override fun deleteNode(deleteRequest: NodeDeleteRequest) {
        with(deleteRequest) {
            deleteByPath(projectId, repoName, fullPath, operator)
        }
    }

    override fun deleteByPath(projectId: String, repoName: String, fullPath: String, operator: String) {
        val normalizedFullPath = normalizeFullPath(fullPath)
        val normalizedPath = toPath(normalizedFullPath)
        val escapedPath = escapeRegex(normalizedPath)
        val query = nodeQuery(projectId, repoName)
        query.addCriteria(
            Criteria().orOperator(
                where(TNode::fullPath).regex("^$escapedPath"),
                where(TNode::fullPath).isEqualTo(normalizedFullPath)
            )
        )
        try {
            nodeDao.updateMulti(query, nodeDeleteUpdate(operator))
        } catch (exception: DuplicateKeyException) {
            logger.warn("Delete node[$projectId/$repoName$fullPath] error: [${exception.message}]")
        }
        logger.info("Delete node [$projectId/$repoName$fullPath] by [$operator] success.")
    }

    /**
     * 将节点重命名为指定名称
     */
    private fun doRename(node: TNode, newFullPath: String, operator: String) {
        val projectId = node.projectId
        val repoName = node.repoName
        val newPath = resolveParent(newFullPath)
        val newName = resolveName(newFullPath)

        // 检查新路径是否被占用
        if (nodeDao.exists(projectId, repoName, newFullPath)) {
            logger.warn("Rename node [${node.fullPath}] failed: $newFullPath is exist.")
            throw ErrorCodeException(ArtifactMessageCode.NODE_EXISTED, newFullPath)
        }

        // 如果为文件夹，查询子节点并修改
        if (node.folder) {
            mkdirs(projectId, repoName, newFullPath, operator)
            val newParentPath = toPath(newFullPath)
            val listOption = NodeListOption(
                includeFolder = true,
                includeMetadata = false,
                deep = false,
                sort = false
            )
            val query = nodeListQuery(projectId, repoName, node.fullPath, listOption)
            nodeDao.find(query).forEach { doRename(it, newParentPath + it.name, operator) }
            // 删除自己
            nodeDao.remove(nodeQuery(projectId, repoName, node.fullPath))
        } else {
            // 修改自己
            val selfQuery = nodeQuery(projectId, repoName, node.fullPath)
            val selfUpdate = nodePathUpdate(newPath, newName, operator)
            nodeDao.updateFirst(selfQuery, selfUpdate)
        }
    }

    /**
     * 递归创建目录
     */
    private fun mkdirs(projectId: String, repoName: String, path: String, createdBy: String) {
        // 格式化
        val fullPath = toFullPath(path)
        if (!nodeDao.exists(projectId, repoName, fullPath)) {
            val parentPath = resolveParent(fullPath)
            val name = resolveName(fullPath)
            mkdirs(projectId, repoName, parentPath, createdBy)
            val node = TNode(
                folder = true,
                path = parentPath,
                name = name,
                fullPath = combineFullPath(parentPath, name),
                size = 0,
                expireDate = null,
                metadata = mutableListOf(),
                projectId = projectId,
                repoName = repoName,
                createdBy = createdBy,
                createdDate = LocalDateTime.now(),
                lastModifiedBy = createdBy,
                lastModifiedDate = LocalDateTime.now()
            )
            doCreate(node)
        }
    }

    /**
     * 处理节点操作请求
     */
    private fun moveOrCopy(request: CrossRepoNodeRequest, operator: String) {
        with(request) {
            // 准备数据
            val srcFullPath = normalizeFullPath(srcFullPath)
            val destProjectId = request.destProjectId ?: srcProjectId
            val destRepoName = request.destRepoName ?: srcRepoName
            val destFullPath = normalizeFullPath(request.destFullPath)

            val isSameRepository = srcProjectId == destProjectId && srcRepoName == destRepoName
            // 查询repository
            val srcRepo = repositoryDao.findByNameAndType(srcProjectId, srcRepoName)
                ?: throw ErrorCodeException(ArtifactMessageCode.REPOSITORY_NOT_FOUND, srcRepoName)
            val destRepo = if (!isSameRepository) {
                repositoryDao.findByNameAndType(destProjectId, destRepoName)
                    ?: throw ErrorCodeException(ArtifactMessageCode.REPOSITORY_NOT_FOUND, destRepoName)
            } else srcRepo

            // 查询storageCredentials
            val srcCredentials = srcRepo.credentialsKey?.let {
                storageCredentialService.findByKey(it)
            }
            val destCredentials = if (!isSameRepository) {
                destRepo.credentialsKey?.let { storageCredentialService.findByKey(it) }
            } else srcCredentials

            // 只允许local或者composite类型仓库操作
            val canSrcRepoMove = srcRepo.category.let {
                it == RepositoryCategory.LOCAL || it == RepositoryCategory.COMPOSITE
            }
            val canDestRepoMove = destRepo.category.let {
                it == RepositoryCategory.LOCAL || it == RepositoryCategory.COMPOSITE
            }
            if (!canSrcRepoMove || !canDestRepoMove) {
                throw ErrorCodeException(CommonMessageCode.OPERATION_UNSUPPORTED)
            }
            val srcNode = nodeDao.findNode(srcProjectId, srcRepoName, srcFullPath)
                ?: throw ErrorCodeException(ArtifactMessageCode.NODE_NOT_FOUND, srcFullPath)
            val destNode = nodeDao.findNode(destProjectId, destRepoName, destFullPath)
            // 同路径，跳过
            if (isSameRepository && srcNode.fullPath == destNode?.fullPath) return
            // src为dest目录下的子节点，跳过
            if (isSameRepository && destNode?.folder == true && srcNode.path == toPath(destNode.fullPath)) return
            // 目录 ->
            if (srcNode.folder) {
                // 目录 -> 文件: error
                if (destNode?.folder == false) {
                    throw ErrorCodeException(ArtifactMessageCode.NODE_CONFLICT, destFullPath)
                }
                val destRootNodePath = if (destNode == null) {
                    // 目录 -> 不存在的目录
                    val path = resolveParent(destFullPath)
                    val name = resolveName(destFullPath)
                    // 创建dest父目录
                    mkdirs(destProjectId, destRepoName, path, operator)
                    // 操作节点
                    doMoveOrCopy(srcNode, destRepo, srcCredentials, destCredentials, path, name, request, operator)
                    combinePath(path, name)
                } else {
                    // 目录 -> 存在的目录
                    val path = toPath(destNode.fullPath)
                    val nodeName = srcNode.name
                    // 操作节点
                    doMoveOrCopy(srcNode, destRepo, srcCredentials, destCredentials, path, nodeName, request, operator)
                    combinePath(path, srcNode.name)
                }
                val srcRootNodePath = toPath(srcNode.fullPath)
                val listOption = NodeListOption(
                    includeFolder = true,
                    includeMetadata = false,
                    deep = true,
                    sort = false
                )
                val query = nodeListQuery(srcNode.projectId, srcNode.repoName, srcRootNodePath, listOption)
                // 目录下的节点 -> 创建好的目录
                nodeDao.find(query).forEach {
                    val destPath = it.path.replaceFirst(srcRootNodePath, destRootNodePath)
                    doMoveOrCopy(it, destRepo, srcCredentials, destCredentials, destPath, null, request, operator)
                }
            } else {
                // 文件 ->
                val destPath = if (destNode?.folder == true) toPath(destNode.fullPath) else resolveParent(destFullPath)
                val destName = if (destNode?.folder == true) srcNode.name else resolveName(destFullPath)
                // 创建dest父目录
                mkdirs(destProjectId, destRepoName, destPath, operator)
                doMoveOrCopy(srcNode, destRepo, srcCredentials, destCredentials, destPath, destName, request, operator)
            }
            // event
            if (request is NodeMoveRequest) {
                publishEvent(NodeMovedEvent(request))
            } else if (request is NodeCopyRequest) {
                publishEvent(NodeCopiedEvent(request))
            }
            logger.info("[${request.getOperateName()}] node success: [$this]")
        }
    }

    /**
     * 移动/拷贝节点
     */
    private fun doMoveOrCopy(
        srcNode: TNode,
        destRepository: TRepository,
        srcStorageCredentials: StorageCredentials?,
        destStorageCredentials: StorageCredentials?,
        destPath: String,
        nodeName: String?,
        request: CrossRepoNodeRequest,
        operator: String
    ) {
        // 计算destName
        val destName = nodeName ?: srcNode.name
        val destFullPath = combineFullPath(destPath, destName)
        // 冲突检查
        val existNode = nodeDao.findNode(destRepository.projectId, destRepository.name, destFullPath)
        // 目录 -> 目录: 跳过
        if (srcNode.folder && existNode?.folder == true) return
        // 目录 -> 文件: 出错
        if (srcNode.folder && existNode?.folder == false) {
            throw ErrorCodeException(ArtifactMessageCode.NODE_CONFLICT, existNode.fullPath)
        }
        // 文件 -> 文件 & 不允许覆盖: 出错
        if (!srcNode.folder && existNode?.folder == false && !request.overwrite) {
            throw ErrorCodeException(ArtifactMessageCode.NODE_CONFLICT, existNode.fullPath)
        }

        // copy目标节点
        val destNode = srcNode.copy(
            id = null,
            projectId = destRepository.projectId,
            repoName = destRepository.name,
            path = destPath,
            name = destName,
            fullPath = destFullPath,
            lastModifiedBy = operator,
            lastModifiedDate = LocalDateTime.now()
        )
        // move操作，create信息保留
        if (request is NodeMoveRequest) {
            destNode.createdBy = operator
            destNode.createdDate = LocalDateTime.now()
        }
        // 文件 -> 文件 & 允许覆盖: 删除old
        if (!srcNode.folder && existNode?.folder == false && request.overwrite) {
            val query = nodeQuery(existNode.projectId, existNode.repoName, existNode.fullPath)
            val update = nodeDeleteUpdate(operator)
            nodeDao.updateFirst(query, update)
        }
        // 文件 & 跨存储
        if (!srcNode.folder && srcStorageCredentials != destStorageCredentials) {
            storageService.copy(srcNode.sha256!!, srcStorageCredentials, destStorageCredentials)
        }
        // 创建dest节点
        doCreate(destNode, destRepository)
        // move操作，创建dest节点后，还需要删除src节点
        // 因为分表所以不能直接更新src节点，必须创建新的并删除旧的
        if (request is NodeMoveRequest) {
            val query = nodeQuery(srcNode.projectId, srcNode.repoName, srcNode.fullPath)
            val update = nodeDeleteUpdate(operator)
            nodeDao.updateFirst(query, update)
        }
    }

    private fun doCreate(node: TNode, repository: TRepository? = null): TNode {
        try {
            nodeDao.insert(node)
            node.takeUnless { it.folder }?.run { fileReferenceService.increment(this, repository) }
        } catch (exception: DuplicateKeyException) {
            logger.warn("Insert node[$node] error: [${exception.message}]")
        }

        return node
    }

    /**
     * 根据有效天数，计算到期时间
     */
    private fun parseExpireDate(expireDays: Long?): LocalDateTime? {
        return expireDays?.takeIf { it > 0 }?.run { LocalDateTime.now().plusDays(this) }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(NodeServiceImpl::class.java)

        private fun convert(tNode: TNode?): NodeInfo? {
            return tNode?.let {
                val metadata = MetadataUtils.toMap(it.metadata)
                NodeInfo(
                    createdBy = it.createdBy,
                    createdDate = it.createdDate.format(DateTimeFormatter.ISO_DATE_TIME),
                    lastModifiedBy = it.lastModifiedBy,
                    lastModifiedDate = it.lastModifiedDate.format(DateTimeFormatter.ISO_DATE_TIME),
                    projectId = it.projectId,
                    repoName = it.repoName,
                    folder = it.folder,
                    path = it.path,
                    name = it.name,
                    fullPath = it.fullPath,
                    size = it.size,
                    sha256 = it.sha256,
                    md5 = it.md5,
                    metadata = metadata
                )
            }
        }

        private fun convertToDetail(tNode: TNode?): NodeDetail? {
            return convert(tNode)?.let { NodeDetail(it) }
        }
    }
}
