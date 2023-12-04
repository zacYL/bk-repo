package com.tencent.bkrepo.repository.service.repo.impl

import com.tencent.bkrepo.common.security.util.SecurityUtils
import com.tencent.bkrepo.job.api.JobClient
import com.tencent.bkrepo.repository.config.RepositoryProperties
import com.tencent.bkrepo.repository.dao.FileReferenceDao
import com.tencent.bkrepo.repository.dao.NodeDao
import com.tencent.bkrepo.repository.dao.RepositoryDao
import com.tencent.bkrepo.repository.job.DeletedNodeCleanupJob
import com.tencent.bkrepo.repository.model.TFileReference
import com.tencent.bkrepo.repository.model.TRepository
import com.tencent.bkrepo.repository.pojo.node.NodeSizeInfo
import com.tencent.bkrepo.repository.pojo.storage.DiskCleanInfo
import com.tencent.bkrepo.repository.service.repo.StorageCleanService
import com.tencent.bkrepo.repository.util.FileSizeUtils
import org.slf4j.LoggerFactory
import org.springframework.data.mongodb.core.query.Criteria
import org.springframework.data.mongodb.core.query.Query
import org.springframework.stereotype.Service

@Service
class StorageCleanServiceImpl(
    private val repositoryDao: RepositoryDao,
    private val nodeDao: NodeDao,
    private val fileReferenceDao: FileReferenceDao,
    private val repositoryProperties: RepositoryProperties,
    private val deletedNodeCleanupJob: DeletedNodeCleanupJob,
    private val jobClient: JobClient
) : StorageCleanService {
    override fun computeCleanDisk(): DiskCleanInfo {
        var total = 0L
        val repoList = repositoryDao.findAll()
        val resultMap = deleteNodeCompute(repoList)
        if (resultMap.isEmpty()) {
            return DiskCleanInfo(freeSpaceSize = FileSizeUtils.formatFileSize(total))
        }
        resultMap.keys.forEach { sha256 ->
            fileReferenceDao.findOne(buildQuery(sha256, null))?.let { fileReference ->
                if (fileReference.count == resultMap[sha256]!!.subNodeCount) total += resultMap[sha256]!!.size
            }
        }
        return DiskCleanInfo(freeSpaceSize = FileSizeUtils.formatFileSize(total))
    }

    override fun executeDiskClean() {
        logger.info("start execute clean disk by ${SecurityUtils.getUserId()}")
        val start = System.currentTimeMillis()
        val deletedNodeReserveDays = repositoryProperties.deletedNodeReserveDays
        repositoryProperties.deletedNodeReserveDays = 0
        deletedNodeCleanupJob.start()
        jobClient.fileReferenceClean()
        repositoryProperties.deletedNodeReserveDays = deletedNodeReserveDays
        logger.info("clean disk execute success, ${System.currentTimeMillis() - start}ms")
    }

    private fun deleteNodeCompute(repoList: List<TRepository>): Map<String, NodeSizeInfo> {
        val resultMap = mutableMapOf<String, NodeSizeInfo>()
        repoList.forEach { repo ->
            val deletedNodeList = nodeDao.findDeleteNode(repo.name)
            if (deletedNodeList.isEmpty()) {
                return@forEach
            }
            deletedNodeList.forEach { deletedNode ->
                val info = resultMap[deletedNode.sha256]
                resultMap[deletedNode.sha256!!] = if (info == null) {
                    NodeSizeInfo(1, deletedNode.size)
                } else {
                    NodeSizeInfo(++info.subNodeCount, info.size)
                }
            }
        }
        return resultMap
    }

    private fun buildQuery(sha256: String, credentialsKey: String?): Query {
        val criteria = Criteria.where(TFileReference::sha256.name).`is`(sha256)
        criteria.and(TFileReference::credentialsKey.name).`is`(credentialsKey)
        return Query.query(criteria)
    }

    companion object {
        private val logger = LoggerFactory.getLogger(StorageCleanServiceImpl::class.java)
    }
}
