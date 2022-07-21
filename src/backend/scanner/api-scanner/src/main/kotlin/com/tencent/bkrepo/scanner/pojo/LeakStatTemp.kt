package com.tencent.bkrepo.scanner.pojo

import com.tencent.bkrepo.scanner.pojo.response.statistics.LeakStat
import java.time.LocalDateTime

data class LeakStatTemp(
    var createdDate: LocalDateTime,
    val leakScanResult: LeakStat
)
