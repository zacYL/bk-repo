package com.tencent.bkrepo.analyst.controller.user

import com.tencent.bkrepo.analyst.pojo.request.statistics.ScanStatRequest
import com.tencent.bkrepo.analyst.pojo.response.statistics.LeakStat
import com.tencent.bkrepo.analyst.pojo.response.statistics.ScanStat
import com.tencent.bkrepo.analyst.service.ScanStatService
import com.tencent.bkrepo.common.api.pojo.Response
import com.tencent.bkrepo.common.service.util.ResponseBuilder
import io.swagger.annotations.Api
import io.swagger.annotations.ApiOperation
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@Api("项目扫描数据统计接口")
@RestController
@RequestMapping("/api/statistics")
class UserStatController(
    private val scanStatService: ScanStatService
) {

    @ApiOperation("查询时间段内每日制品扫描数与质量规则触发数")
    @GetMapping("/summary")
    fun scanStat(request: ScanStatRequest): Response<ScanStat> {
        return ResponseBuilder.success(scanStatService.querySummary(request))
    }

    @ApiOperation("查询时间段内不同分级的漏洞数量")
    @GetMapping("/leak/summary")
    fun leakStat(request: ScanStatRequest): Response<LeakStat> {
        return ResponseBuilder.success(scanStatService.queryLeaks(request))
    }
}
