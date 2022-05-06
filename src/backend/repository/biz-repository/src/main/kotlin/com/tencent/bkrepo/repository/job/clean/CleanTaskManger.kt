package com.tencent.bkrepo.repository.job.clean

import com.tencent.bkrepo.common.artifact.pojo.configuration.clean.CleanStatus
import com.tencent.bkrepo.repository.config.RepositoryProperties
import com.tencent.bkrepo.repository.dao.RepositoryDao
import com.tencent.bkrepo.repository.service.repo.RepositoryService
import org.quartz.*
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

/**
 * 清理任务管理类
 */
@Service
class CleanTaskManger(
    private val repositoryService: RepositoryService,
    private val taskScheduler: CleanRepoTaskScheduler,
    private val repositoryDao: RepositoryDao
) {
    @Autowired
    lateinit var repositoryProperties: RepositoryProperties

    /**
     * 创建仓库清理job
     */
    fun createCleanJob(projectId: String, repoName: String) {
        val tRepository = repositoryDao.findByNameAndType(projectId, repoName)
        try {
            requireNotNull(tRepository) { "repository:[$repoName] does not exist in projectId:[$projectId]" }
            requireNotNull(tRepository.id) { "projectId:[$projectId] repoName:[$repoName] TRepository id is null" }
        } catch (ex: IllegalArgumentException) {
            logger.error(
                "projectId:[$projectId] repoName:[$repoName] " +
                        "create repo clean job error illegal argument exception:[$ex]"
            )
            return
        }
        val id = tRepository.id!!
        val cleanStrategy = repositoryService.getRepoCleanStrategy(projectId, repoName)
        cleanStrategy?.let {
            //开启,不存在，创建
            if (it.autoClean && !taskScheduler.exist(id)) {
                val jobDetail = createJobDetail(id)
                val trigger = createTrigger(id, repositoryProperties.cleanStrategyTime)
                taskScheduler.scheduleJob(jobDetail, trigger)
            }
            // 关闭，存在，任务状态为 WAITING，则删除；
            // 任务状态为 RUNNING，这里不做处理
            if (!it.autoClean && taskScheduler.exist(id) && it.status == CleanStatus.WAITING) {
                taskScheduler.deleteJob(id)
            }
        } ?: logger.warn("projectId:[$projectId] repoName:[$repoName] clean strategy is null")
    }

    /**
     * 根据任务信息创建job detail
     */
    private fun createJobDetail(id: String): JobDetail {
        return JobBuilder.newJob(CleanRepoJob::class.java)
            .withIdentity(id, CLEAN_JOB_GROUP)
            .requestRecovery()
            .build()
    }

    /**
     * 根据任务信息创建job trigger
     */
    private fun createTrigger(id: String, cronExpression: String): Trigger {
        return TriggerBuilder.newTrigger().withIdentity(id, CLEAN_JOB_GROUP)
            .withSchedule(CronScheduleBuilder.cronSchedule(cronExpression))
            .build()
    }

    companion object {

        private val logger = LoggerFactory.getLogger(CleanTaskManger::class.java)

        /**
         * quartz scheduler的job group名称
         */
        const val CLEAN_JOB_GROUP = "CLEAN"
    }
}
