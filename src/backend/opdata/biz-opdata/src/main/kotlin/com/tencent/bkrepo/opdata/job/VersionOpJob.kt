package com.tencent.bkrepo.opdata.job

import com.tencent.bkrepo.opdata.pojo.VersionOpUpdateRequest
import com.tencent.bkrepo.opdata.service.VersionOpService
import com.tencent.bkrepo.repository.api.PackageStatisticsClient
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import java.time.LocalDateTime

@Component
class VersionOpJob(
    private val packageStatisticsClient: PackageStatisticsClient,
    private val versionOpService: VersionOpService
) {

    // 0 0 4 * * ?
    @Scheduled(cron = "0 0 4 * * ?")
    @SchedulerLock(name = "VersionOpJob", lockAtMostFor = "PT2H")
    fun execute() {
        val limitTime = versionOpService.getLatestModifiedTime() ?: LocalDateTime.MIN
        var pageNumber = 1
        while (true) {
            val versionList = packageStatisticsClient.packageModifiedLimitByTime(
                limitTime, pageNumber, pageSize
            ).data
            if (versionList.isNullOrEmpty()) break
            for (version in versionList) {
                version?.let {
                    versionOpService.update(
                        VersionOpUpdateRequest(
                            projectId = it.projectId,
                            repoName = it.repoName,
                            packageKey = it.key,
                            type = it.type,
                            lastModifiedDate = it.lastModifiedDate,
                            packageId = it.packageId,
                            packageName = it.packageName,
                            packageVersion = it.name,
                            size = it.size,
                            downloads = it.downloads
                        )
                    )
                }
            }
            pageNumber++
        }
    }

    companion object {
        const val pageSize = 1000
    }
}
