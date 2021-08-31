package com.tencent.bkrepo.opdata.job

import com.tencent.bkrepo.opdata.pojo.RepositoryOpUpdateRequest
import com.tencent.bkrepo.opdata.service.RepositoryOpService
import com.tencent.bkrepo.repository.api.RepositoryClient
import com.tencent.bkrepo.repository.api.NodeClient
import com.tencent.bkrepo.repository.api.PackageStatisticsClient
import com.tencent.bkrepo.repository.api.OperateLogClient
import com.tencent.bkrepo.repository.api.ProjectClient
import com.tencent.bkrepo.repository.pojo.repo.RepositoryInfo
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import org.springframework.util.StopWatch

@Component
class RepositoryOpJob(
    private val repositoryClient: RepositoryClient,
    private val nodeClient: NodeClient,
    private val packageStatisticsClient: PackageStatisticsClient,
    private val operateLogClient: OperateLogClient,
    private val repositoryOpService: RepositoryOpService,
    private val projectClient: ProjectClient
) {

    // 0 0 4 * * ?
    @Scheduled(cron = "0 0 4 * * ?")
    @SchedulerLock(name = "RepositoryOpJob", lockAtMostFor = "PT3H")
    fun execute() {
        val watch = StopWatch("RepositoryOpJob").apply { this.start() }
        val projectList = projectClient.listProject().data ?: return
        for (project in projectList) {
            val repoList = repositoryClient.listRepo(project.name).data ?: continue
            repoList.forEach { repositoryStatistics(it) }
        }
        watch.apply {
            this.stop()
            logger.info("$this")
        }
    }

    private fun repositoryStatistics(repo: RepositoryInfo?) {
        repo ?: return
        val usedCapacity = nodeClient.capacity(repo.projectId, repo.name).data ?: 0L
        val packages = packageStatisticsClient.packageTotal(repo.projectId, repo.name).data ?: 0L
        val downloads = operateLogClient.downloads(repo.projectId, repo.name, null).data ?: 0L
        val uploads = operateLogClient.uploads(repo.projectId, repo.name, null).data ?: 0L
        repositoryOpService.update(
            RepositoryOpUpdateRequest(
                projectId = repo.projectId,
                repoName = repo.name,
                repoType = repo.type,
                visits = downloads + uploads,
                downloads = downloads,
                uploads = uploads,
                packages = packages,
                usedCapacity = usedCapacity
            )
        )
    }

    companion object {
        private val logger: Logger = LoggerFactory.getLogger(RepositoryOpJob::class.java)
    }
}
