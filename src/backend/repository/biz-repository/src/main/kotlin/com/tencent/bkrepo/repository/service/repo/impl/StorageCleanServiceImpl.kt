package com.tencent.bkrepo.repository.service.repo.impl

import com.tencent.bkrepo.common.api.exception.ErrorCodeException
import com.tencent.bkrepo.common.artifact.message.ArtifactMessageCode
import com.tencent.bkrepo.common.security.util.SecurityUtils
import com.tencent.bkrepo.job.api.JobClient
import com.tencent.bkrepo.repository.config.RepositoryProperties
import com.tencent.bkrepo.repository.dao.FileReferenceDao
import com.tencent.bkrepo.repository.dao.NodeDao
import com.tencent.bkrepo.repository.dao.ShedLockDao
import com.tencent.bkrepo.repository.job.DeletedNodeCleanupJob
import com.tencent.bkrepo.repository.model.TFileReference
import com.tencent.bkrepo.repository.model.TShedLock
import com.tencent.bkrepo.repository.pojo.node.NodeSizeInfo
import com.tencent.bkrepo.repository.pojo.project.ProjectInfo
import com.tencent.bkrepo.repository.pojo.storage.DiskCleanInfo
import com.tencent.bkrepo.repository.service.repo.ProjectService
import com.tencent.bkrepo.repository.service.repo.StorageCleanService
import com.tencent.bkrepo.repository.util.FileSizeUtils
import org.slf4j.LoggerFactory
import org.springframework.data.mongodb.core.query.Criteria
import org.springframework.data.mongodb.core.query.Query
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Service
import java.time.LocalDateTime

@Service
class StorageCleanServiceImpl(
    private val shedLockDao: ShedLockDao,
    private val nodeDao: NodeDao,
    private val fileReferenceDao: FileReferenceDao,
    private val projectService: ProjectService,
    private val repositoryProperties: RepositoryProperties,
    private val deletedNodeCleanupJob: DeletedNodeCleanupJob,
    private val jobClient: JobClient
) : StorageCleanService {
    override fun computeCleanDisk(): DiskCleanInfo {
        var total = 0L
        val projectList = projectService.listProject()
        val resultMap = deleteNodeCompute(projectList)
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
        // 检测任务是否被锁定（锁定则抛异常）
        val shedLockList = shedLockDao.findAll()
        checkShedLock(shedLockList, deletedNodeCleanupJobName)
        checkShedLock(shedLockList, fileReferenceCleanupJobName)
        // 磁盘清理
        diskClean()
    }

    private fun deleteNodeCompute(projectList: List<ProjectInfo>): Map<String, NodeSizeInfo> {
        val resultMap = mutableMapOf<String, NodeSizeInfo>()
        projectList.forEach { project ->
            val deletedNodeList = nodeDao.findDeleteNode(project.name)
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

    private fun checkShedLock(shedLockList: List<TShedLock>, jobName: String) {
        val currentTime = LocalDateTime.now()
        val shedLock = shedLockList.filter { it.id == jobName }
        shedLock.forEach {
            if (currentTime.isAfter(it.lockedAt) && currentTime.isBefore(it.lockUntil)) {
                throw ErrorCodeException(ArtifactMessageCode.ARTIFACT_TASK_LOCK, jobName)
            }
        }
    }
    @Async
    fun diskClean() {
        logger.info("start execute clean disk by ${SecurityUtils.getUserId()}")
        val start = System.currentTimeMillis()
        val deletedNodeReserveDays = repositoryProperties.deletedNodeReserveDays
        repositoryProperties.deletedNodeReserveDays = 0
        deletedNodeCleanupJob.start()
        jobClient.fileReferenceClean()
        repositoryProperties.deletedNodeReserveDays = deletedNodeReserveDays
        logger.info("clean disk execute success, ${System.currentTimeMillis() - start}ms")
    }

    companion object {
        const val deletedNodeCleanupJobName = "DeletedNodeCleanupJob"
        const val fileReferenceCleanupJobName = "FileReferenceCleanupJob"
        private val logger = LoggerFactory.getLogger(StorageCleanServiceImpl::class.java)
    }
}
