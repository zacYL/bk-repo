package com.tencent.bkrepo.opdata.service

import com.tencent.bkrepo.common.artifact.event.base.EventType
import com.tencent.bkrepo.repository.pojo.bksoftware.DayMetricRequest
import com.tencent.bkrepo.repository.pojo.bksoftware.DayMetricsData
import java.time.LocalDate

interface DayMetricService {
    fun getLatestModifiedTime(): LocalDate?

    fun add(dayMetricRequest: DayMetricRequest)

    fun list(projectId: String?, repoName: String?, days: Long, type: List<EventType>): List<DayMetricsData?>
}
