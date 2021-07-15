package com.tencent.bkrepo.repository.service.bksoftware

import com.tencent.bkrepo.repository.pojo.log.OperateType
import com.tencent.bkrepo.repository.pojo.bksoftware.DayMetricsData
import com.tencent.bkrepo.repository.pojo.bksoftware.DayMetricRequest

interface DayMetricService {
    fun add(dayMetricRequest: DayMetricRequest)

    fun list(projectId: String?, repoName: String?, days: Long, type: Array<OperateType>): List<DayMetricsData?>
}
