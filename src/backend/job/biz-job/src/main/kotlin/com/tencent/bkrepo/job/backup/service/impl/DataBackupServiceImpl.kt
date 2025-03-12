package com.tencent.bkrepo.job.backup.service.impl

import com.tencent.bkrepo.common.api.exception.BadRequestException
import com.tencent.bkrepo.common.api.message.CommonMessageCode
import com.tencent.bkrepo.common.api.pojo.Page
import com.tencent.bkrepo.common.mongo.util.Pages
import com.tencent.bkrepo.common.security.util.SecurityUtils
import com.tencent.bkrepo.job.DATA_RECORDS_BACKUP
import com.tencent.bkrepo.job.DATA_RECORDS_RESTORE
import com.tencent.bkrepo.job.backup.dao.BackupTaskDao
import com.tencent.bkrepo.job.backup.event.TaskCreateContext
import com.tencent.bkrepo.job.backup.event.TaskCreateEvent
import com.tencent.bkrepo.job.backup.model.TBackupTask
import com.tencent.bkrepo.job.backup.pojo.BackupTaskState
import com.tencent.bkrepo.job.backup.pojo.record.BackupContext
import com.tencent.bkrepo.job.backup.pojo.task.BackupTask
import com.tencent.bkrepo.job.backup.pojo.task.BackupTask.Companion.toDto
import com.tencent.bkrepo.job.backup.pojo.task.BackupTaskOption
import com.tencent.bkrepo.job.backup.pojo.task.BackupTaskRequest
import com.tencent.bkrepo.job.backup.service.DataBackupService
import com.tencent.bkrepo.job.backup.service.DataRecordsBackupService
import com.tencent.bkrepo.job.backup.service.DataRecordsRestoreService
import org.slf4j.LoggerFactory
import org.springframework.context.ApplicationContext
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Service
import java.io.FileNotFoundException
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.time.LocalDateTime
import java.util.regex.Pattern

@Service
class DataBackupServiceImpl(
    private val backupTaskDao: BackupTaskDao,
    private val dataRecordsBackupService: DataRecordsBackupService,
    private val dataRecordsRestoreService: DataRecordsRestoreService,
    private val applicationContext: ApplicationContext,
    private val heartbeatManager: HeartbeatManager,
) : DataBackupService {
    override fun createTask(taskRequest: BackupTaskRequest): String {
        contentCheck(taskRequest)
        val task = buildBackupTask(taskRequest)
        val saveTask = backupTaskDao.save(task)
        applicationContext.publishEvent(
            TaskCreateEvent(
                context = TaskCreateContext(
                    taskId = saveTask.id!!,
                )
            )
        )
        return saveTask.id
    }

    override fun executeTask(taskId: String) {
        val task = backupTaskDao.findTasksById(taskId)
            ?: throw BadRequestException(CommonMessageCode.PARAMETER_INVALID, "taskId")
        heartbeatManager.startHeartbeat(taskId)
        try {
            if (task.state != BackupTaskState.PENDING.name)
                throw BadRequestException(CommonMessageCode.PARAMETER_INVALID, "state")
            if (task.type == DATA_RECORDS_BACKUP) {
                val context = BackupContext(task = task)
                dataRecordsBackupService.projectDataBackup(context)
            } else {
                val context = BackupContext(task = task)
                dataRecordsRestoreService.projectDataRestore(context)
            }
        } finally {
            heartbeatManager.stopHeartbeat(taskId)
        }
    }

    override fun findTasks(option: BackupTaskOption, pageRequest: PageRequest): Page<BackupTask> {
        val count = backupTaskDao.count(option)
        val records = backupTaskDao.find(option, pageRequest).map { it.toDto() }
        return Pages.ofResponse(pageRequest, count, records)
    }

    override fun getTaskDetail(taskId: String): BackupTask {
        return backupTaskDao.findTasksById(taskId)?.toDto() ?: throw BadRequestException(
            CommonMessageCode.PARAMETER_INVALID,
            "taskId"
        )
    }

    private fun contentCheck(request: BackupTaskRequest) {
        with(request) {
            isValidLinuxPath(request.storeLocation)
            duplicateNameCheck(request.name, request.type)
            if (content == null || content!!.projects.isNullOrEmpty()) {
                logger.warn("backup content [$content] is illegal!")
                throw BadRequestException(CommonMessageCode.PARAMETER_INVALID, BackupTaskRequest::content.name)
            }
            try {
                val targetFile = Paths.get(storeLocation)
                storeLocationHandler(request.type, targetFile)
                if (!targetFile.toFile().exists()) throw FileNotFoundException(storeLocation)
            } catch (e: Exception) {
                logger.warn("backup store location [$storeLocation] is illegal!")
                throw BadRequestException(
                    CommonMessageCode.PARAMETER_INVALID,
                    BackupTaskRequest::storeLocation.name
                )
            }
        }
    }


    private fun storeLocationHandler(type: String, targetFile: Path) {
        when (type) {
            DATA_RECORDS_BACKUP -> {
                try {
                    Files.createDirectories(targetFile)
                } catch (e: Exception) {
                    logger.warn("backup store location [$targetFile] is illegal!")
                    throw BadRequestException(
                        CommonMessageCode.PARAMETER_INVALID,
                        BackupTaskRequest::storeLocation.name
                    )
                }
                return
            }

            DATA_RECORDS_RESTORE -> {
                return
            }

            else -> {
                logger.warn("task type [$type] is illegal!")
                throw BadRequestException(CommonMessageCode.PARAMETER_INVALID, BackupTaskRequest::type.name)
            }
        }
    }

    private fun buildBackupTask(request: BackupTaskRequest): TBackupTask {
        return TBackupTask(
            name = request.name,
            storeLocation = request.storeLocation,
            type = request.type,
            content = request.content,
            createdBy = SecurityUtils.getUserId(),
            createdDate = LocalDateTime.now(),
            lastModifiedDate = LocalDateTime.now(),
            lastModifiedBy = SecurityUtils.getUserId(),
            backupSetting = request.backupSetting
        )
    }

    private fun duplicateNameCheck(name: String, type: String) {
        if (type == DATA_RECORDS_BACKUP) {
            if (backupTaskDao.findTasksByName(name, type) != null) {
                throw BadRequestException(CommonMessageCode.RESOURCE_EXISTED, name)
            }
        }
    }

    fun isValidLinuxPath(path: String) {
        val pattern = Pattern.compile("^\\/([a-zA-Z0-9+, ._-]+\\/)*[a-zA-Z0-9+, ._-]+$")
        val matcher = pattern.matcher(path)
        if (!matcher.matches()) throw BadRequestException(CommonMessageCode.PARAMETER_INVALID, path)

        if (path.length > 4096) throw BadRequestException(CommonMessageCode.PARAMETER_INVALID, path)

        val parts = path.split("/")
        parts.forEach {
            if (it == "." || it == "..") throw BadRequestException(CommonMessageCode.PARAMETER_INVALID, path)
            if (it.length > 256) throw BadRequestException(CommonMessageCode.PARAMETER_INVALID, path)
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(DataBackupServiceImpl::class.java)
    }
}