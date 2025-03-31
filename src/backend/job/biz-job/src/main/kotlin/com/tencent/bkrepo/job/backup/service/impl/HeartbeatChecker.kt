package com.tencent.bkrepo.job.backup.service.impl

import com.tencent.bkrepo.job.backup.dao.BackupTaskDao
import com.tencent.bkrepo.job.backup.pojo.BackupTaskState
import com.tencent.bkrepo.job.backup.pojo.task.BackupTaskOption
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import java.time.Duration
import java.time.Instant
import java.time.LocalDateTime

@Component
class HeartbeatChecker(
    private val backupTaskDao: BackupTaskDao,
) {
    companion object {
        private val logger = LoggerFactory.getLogger(HeartbeatChecker::class.java)
        private const val CHECK_INTERVAL_MS = 10_000L // 每10秒检测一次
    }

    @Scheduled(fixedDelay = CHECK_INTERVAL_MS)
    fun checkStalledTasks() {
        val now = Instant.now()
        backupTaskDao.find(BackupTaskOption(state = BackupTaskState.RUNNING.name)).forEach { task ->
            task.lastHeartbeat?.let { last ->
                if (Duration.between(last, now).seconds > 60) {
                    logger.warn("Task [${task.id}] heartbeat timeout, marking as failed")
                    backupTaskDao.updateState(
                        task.id!!,
                        BackupTaskState.FAILURE,
                        endDate = LocalDateTime.now()
                    )
                }
            } ?: run {
                // 从未发送心跳的任务直接标记失败
                logger.warn("Task [${task.id}] has no heartbeat, marking as failed")
                backupTaskDao.updateState(
                    task.id!!,
                    BackupTaskState.FAILURE,
                    endDate = LocalDateTime.now()
                )
            }
        }
    }
}