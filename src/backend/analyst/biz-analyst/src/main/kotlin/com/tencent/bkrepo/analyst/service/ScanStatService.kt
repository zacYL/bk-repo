package com.tencent.bkrepo.analyst.service

import com.tencent.bkrepo.analyst.pojo.request.statistics.ScanStatRequest
import com.tencent.bkrepo.analyst.pojo.response.statistics.LeakStat
import com.tencent.bkrepo.analyst.pojo.response.statistics.ScanStat


interface ScanStatService {
    /**
     * 查询扫描统计数据总览
     */
    fun querySummary(request: ScanStatRequest): ScanStat

    /**
     * 查询扫描结果中各分级的漏洞统计数据
     */
    fun queryLeaks(request: ScanStatRequest): LeakStat
}
