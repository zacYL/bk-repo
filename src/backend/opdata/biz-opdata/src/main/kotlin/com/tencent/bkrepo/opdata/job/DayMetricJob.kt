package com.tencent.bkrepo.opdata.job

import com.tencent.bkrepo.common.artifact.pojo.RepositoryType
import com.tencent.bkrepo.opdata.service.DayMetricService
import com.tencent.bkrepo.repository.api.OperateLogClient
import com.tencent.bkrepo.repository.pojo.bksoftware.DayMetricRequest
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import java.time.LocalDate

@Component
class DayMetricJob(
    private val operateLogClient: OperateLogClient,
    private val dayMetricService: DayMetricService
) {

    // 0 0 4 * * ?
    @Scheduled(cron = "0 0 4 * * ?")
    @SchedulerLock(name = "DayMetricJob", lockAtMostFor = "PT2H")
    fun execute() {
        val limitTime = dayMetricService.getLatestModifiedTime() ?: LocalDate.MIN
        var pageNumber = 1
        while (true) {
            val operateLogList = operateLogClient.operateLogLimitByTime(
                limitTime.atStartOfDay(), pageNumber, pageSize
            ).data
            if (operateLogList.isNullOrEmpty()) break
            for (log in operateLogList) {
                if (!log.projectId.isNullOrBlank() && !log.repoName.isNullOrBlank()) {

                    dayMetricService.add(
                        DayMetricRequest(
                            day = log.createdDate.toLocalDate(),
                            projectId = log.projectId!!,
                            repoName = log.repoName!!,
                            type = log.type,
                            repoType = RepositoryType.valueOf(
                                log.description["packageType"] as? String ?: "GENERIC"
                            )
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
