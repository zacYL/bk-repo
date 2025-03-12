package com.tencent.bkrepo.job.backup.event

import com.tencent.bkrepo.job.backup.dao.BackupTaskDao
import com.tencent.bkrepo.job.backup.pojo.setting.BackupExecutionStrategy
import com.tencent.bkrepo.job.backup.service.DataBackupService
import org.slf4j.LoggerFactory
import org.springframework.context.event.EventListener
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Component

@Component
class TaskCreateEventListener(
    private val dataBackupService: DataBackupService,
    private val backupTaskDao: BackupTaskDao,

    ) {

    companion object{

        private val logger = LoggerFactory.getLogger(TaskCreateEventListener::class.java)
    }
    @Async
    @EventListener
    fun onAdminCreateEvent(event: TaskCreateEvent) {
        with(event.context) {
            val task = backupTaskDao.findTasksById(taskId) ?: return
            when (task.backupSetting.executionStrategy) {
                BackupExecutionStrategy.IMMEDIATELY -> {
                    if (task.backupSetting.executionPlan.executeImmediately){
                        logger.info("taskId:[$taskId] is executed immediately")
                        dataBackupService.executeTask(taskId)
                    }
                }

                else -> {
                    //TODO 其他执行策略处理
                }
            }
        }
    }
}
