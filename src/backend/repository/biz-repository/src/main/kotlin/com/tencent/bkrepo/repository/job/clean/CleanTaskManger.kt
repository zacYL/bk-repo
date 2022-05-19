package com.tencent.bkrepo.repository.job.clean

import com.tencent.bkrepo.common.artifact.pojo.configuration.clean.CleanStatus
import com.tencent.bkrepo.repository.config.RepositoryProperties
import com.tencent.bkrepo.repository.dao.RepositoryDao
import com.tencent.bkrepo.repository.model.TRepository
import com.tencent.bkrepo.repository.service.repo.RepositoryService
import org.quartz.*
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.scheduling.annotation.Scheduled
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
     * 查询所有仓库（本地仓库 & 组合仓库）
     * 遍历仓库，获取仓库的 cleanStrategy
     * 判断【autoClean】
     *      true：判断job是否存在，不存在，创建；存在，不处理
     *      false：判断job是否存在，存在，删除；不存在，不处理
     */
    @Scheduled(initialDelay = RELOAD_INITIAL_DELAY, fixedDelay = RELOAD_FIXED_DELAY)
    fun reloadCleanTask() {
        var skip = 0L
        var repoList = repositoryService.allRepoPage(skip)
        while (repoList.isNotEmpty()) {
            skip += repoList.size
            repoList.forEach { repo ->
                createCleanJob(repo)
            }
            repoList = repositoryService.allRepoPage(skip)
        }
        if (logger.isDebugEnabled) {
            logger.debug("Success to reload clean task, now exist job list:[${taskScheduler.listJobKeys()}]")
        }
    }

    fun createCleanJob(repo: TRepository) {
        val repoId = repo.id!!
        val cleanStrategy = repositoryService.getRepoCleanStrategy(repo.projectId, repo.name)
        cleanStrategy?.let {
            //开启，不存在，创建
            if (it.autoClean && !taskScheduler.exist(repoId)) {
                val jobDetail = createJobDetail(repoId)
                val trigger = createTrigger(repoId, repositoryProperties.cleanStrategyTime)
                taskScheduler.scheduleJob(jobDetail, trigger)
            }
            // 关闭，存在，任务状态为 WAITING，则删除；
            // 任务状态为 RUNNING，这里不做处理
            if (!it.autoClean && taskScheduler.exist(repoId) && it.status == CleanStatus.WAITING) {
                taskScheduler.deleteJob(repoId)
            }
        } ?: logger.info("projectId:[${repo.projectId}] repoName:[${repo.name}] clean strategy is null")
    }

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

        /**
         * 进程启动后加载任务延迟时间
         */
        private const val RELOAD_INITIAL_DELAY = 20 * 1000L

        /**
         * 重新加载任务固定延迟时间
         */
        //TODO 间隔加载时间大小设置
        private const val RELOAD_FIXED_DELAY = 300 * 1000L
    }
}
