package com.tencent.bkrepo.job.backup.service.impl

import com.tencent.bkrepo.job.backup.dao.BackupTaskDao
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import java.time.Instant
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.TimeUnit
import javax.annotation.PreDestroy

@Component
class HeartbeatManager(
    private val backupTaskDao: BackupTaskDao,
) {
    companion object {
        private val logger = LoggerFactory.getLogger(HeartbeatManager::class.java)
        private const val HEARTBEAT_INTERVAL_SECONDS = 5L
    }

    private val runningTasks = ConcurrentHashMap<String, ScheduledFuture<*>>()

    // 提交任务时启动心跳
    fun startHeartbeat(taskId: String) {
        val scheduler = Executors.newSingleThreadScheduledExecutor()
        val future = scheduler.scheduleAtFixedRate(
            { updateHeartbeat(taskId) },
            0, HEARTBEAT_INTERVAL_SECONDS, TimeUnit.SECONDS
        )
        runningTasks[taskId] = future
        logger.info("Heartbeat started for task [$taskId]")
    }

    // 停止心跳
    fun stopHeartbeat(taskId: String) {
        runningTasks[taskId]?.apply {
            cancel(true)
            runningTasks.remove(taskId)
            logger.info("Heartbeat stopped for task [$taskId]")
        }
    }

    private fun updateHeartbeat(taskId: String) {
        try {
            backupTaskDao.updateLastHeartbeat(taskId, Instant.now())
        } catch (e: Exception) {
            logger.error("Update heartbeat failed for task [$taskId]", e)
        }
    }

    // 服务关闭时清理资源
    @PreDestroy
    fun shutdown() {
        runningTasks.values.forEach { it.cancel(true) }
        logger.info("Heartbeat manager shutdown completed")
    }
}