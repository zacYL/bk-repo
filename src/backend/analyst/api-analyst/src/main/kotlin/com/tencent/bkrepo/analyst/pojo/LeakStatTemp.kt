package com.tencent.bkrepo.analyst.pojo

import com.tencent.bkrepo.analyst.pojo.response.statistics.LeakStat
import java.time.LocalDateTime

data class LeakStatTemp(
    var createdDate: LocalDateTime,
    val leakScanResult: LeakStat
)
