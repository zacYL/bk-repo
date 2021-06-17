package com.tencent.bkrepo.repository.pojo.metric

import java.time.LocalDate

data class DayMetric(
    val id: LocalDate,
    var projectId: String?,
    var repoName: String?,
    var count: Long
)
