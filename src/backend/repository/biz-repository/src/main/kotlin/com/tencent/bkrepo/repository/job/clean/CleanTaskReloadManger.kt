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
class CleanTaskReloadManger(
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
        try {
            logger.info("create clean strategy job , params: projectId:[$projectId] repoName:[$repoName]")
            val tRepository = repositoryDao.findByNameAndType(projectId, repoName)
            requireNotNull(tRepository) { "repository:[$repoName] does not exist in projectId:[$projectId]" }
            val id = tRepository.id
            requireNotNull(id) { "projectId:[$projectId] repoName:[$repoName] TRepository id is null" }
            val cleanStrategy = repositoryService.getRepoCleanStrategy(projectId, repoName)
            if (cleanStrategy != null) {
                with(cleanStrategy) {
                    logger.info("clean strategy autoClean:[${autoClean} job status:[${status}]")
                    if (autoClean) {
                        when (status) {
                            CleanStatus.WAITING -> {
                                val jobDetail = createJobDetail(id)
                                val trigger = createTrigger(id, repositoryProperties.cleanStrategyTime)
                                taskScheduler.scheduleJob(jobDetail, trigger)
                                //修改【清理策略】中的任务状态为 【RUNNING】
                                repositoryService.updateRepoCleanStrategyStatus(projectId, tRepository.name)
                                logger.info("create jobDetail, trigger and add schedule , jobKey:[$id]")
                            }
                            CleanStatus.RUNNING -> {
                                if (!taskScheduler.exist(id)) {
                                    logger.warn("status is RUNNING but job is not exist ,jobKey:[$id]")
                                    val jobDetail = createJobDetail(id)
                                    val trigger = createTrigger(id, repositoryProperties.cleanStrategyTime)
                                    taskScheduler.scheduleJob(jobDetail, trigger)
                                    logger.info("create jobDetail, trigger and add schedule , jobKey:[$id]")
                                } else {
                                    logger.info("job is running jobKey:[$id]")
                                }
                            }
                        }
                    } else {
                        when (status) {
                            CleanStatus.WAITING -> {
                                if (taskScheduler.exist(id)){
                                    logger.warn("status is WAITING but job is exist, jobKey:[$id]")
                                    //任务已存在，且关闭【自动清理】，则删除定时任务
                                    taskScheduler.deleteJob(id)
                                    logger.info("auto clean close, delete exist job, jobKey:[$id]")
                                }
                            }
                            CleanStatus.RUNNING -> {
                                if (!taskScheduler.exist(id)) {
                                    //任务已存在，且关闭【自动清理】，则删除定时任务
                                    taskScheduler.deleteJob(id)
                                    //修改【清理策略】中的任务状态为 【WAITING】
                                    repositoryService.updateRepoCleanStrategyStatus(projectId, tRepository.name)
                                    logger.info("auto clean close, delete exist job, jobKey:[$id]")
                                } else {
                                    logger.warn("job is not exist but status running jobKey:[$id]")
                                    //修改【清理策略】中的任务状态为 【WAITING】
                                    repositoryService.updateRepoCleanStrategyStatus(projectId, tRepository.name)
                                }
                            }
                        }
                    }
                }
            } else {
                logger.warn("projectId:[$projectId] repoName:[$repoName] clean strategy is null")
            }
        }catch (e:Exception){
            logger.error("create repo clean job error:[${e}]")
        }
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

        private val logger = LoggerFactory.getLogger(CleanTaskReloadManger::class.java)

        /**
         * quartz scheduler的job group名称
         */
        const val CLEAN_JOB_GROUP = "CLEAN"
    }
}
