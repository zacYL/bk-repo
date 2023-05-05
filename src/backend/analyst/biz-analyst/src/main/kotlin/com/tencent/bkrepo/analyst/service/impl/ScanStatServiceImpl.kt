package com.tencent.bkrepo.analyst.service.impl

import com.tencent.bkrepo.analyst.dao.ArchiveSubScanTaskDao
import com.tencent.bkrepo.analyst.dao.PlanArtifactLatestSubScanTaskDao
import com.tencent.bkrepo.analyst.pojo.LeakStatTemp
import com.tencent.bkrepo.analyst.pojo.request.SubtaskInfoRequest
import com.tencent.bkrepo.analyst.pojo.request.statistics.ScanStatRequest
import com.tencent.bkrepo.analyst.pojo.response.statistics.LeakStat
import com.tencent.bkrepo.analyst.pojo.response.statistics.ScanDetail
import com.tencent.bkrepo.analyst.pojo.response.statistics.ScanStat
import com.tencent.bkrepo.analyst.service.ScanStatService
import com.tencent.bkrepo.common.analysis.pojo.scanner.CveOverviewKey
import com.tencent.bkrepo.common.analysis.pojo.scanner.SubScanTaskStatus
import org.springframework.stereotype.Service
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.temporal.ChronoUnit

@Service
class ScanStatServiceImpl(
    private val archiveSubScanTaskDao: ArchiveSubScanTaskDao,
    private val planArtifactLatestSubScanTaskDao: PlanArtifactLatestSubScanTaskDao
) : ScanStatService {

    override fun querySummary(request: ScanStatRequest): ScanStat {
        with(request) {
            // 初始化统计值
            val scanStat = ScanStat(dailyStatisticsDetails = buildDetailList(startTime, endTime))
            // 构建查询请求
            val subtaskInfoRequest = SubtaskInfoRequest(
                projectId,
                startDateTime = startTime.atStartOfDay(),
                endDateTime = endTime.atTime(LocalTime.MAX),
                subScanTaskStatus = listOf(SubScanTaskStatus.SUCCESS).map { it.name },
                pageSize = PAGE_SIZE
            )
            return calculateScanStat(subtaskInfoRequest, scanStat)
        }
    }

    override fun queryLeaks(request: ScanStatRequest): LeakStat {
        with(request) {
            val startDateTime = startTime.atStartOfDay()
            val endDateTime = endTime.atTime(LocalTime.MAX)
            return calculateLeaks(projectId, startDateTime, endDateTime)
        }
    }

    private fun calculateLeaks(projectId: String, startDateTime: LocalDateTime, endDateTime: LocalDateTime): LeakStat {
        // 以sha256为key，存储统计过程中所有制品的最新日期记录的漏洞数据
        val statMap = mutableMapOf<String, LeakStatTemp>()
        var pageNumber = 1
        val cveKeys = CveOverviewKey.values().map { it.key }.toSet()
        // 循环分页统计
        while (true) {
            val records = planArtifactLatestSubScanTaskDao.pageBy(
                projectId, startDateTime, endDateTime, pageNumber ++, PAGE_SIZE
            )
            if (records.isEmpty()) {
                break
            }
            records.forEach {
                val scanResult = it.scanResultOverview
                // 扫描结果不包含漏洞数据时跳过本条记录
                if ((scanResult == null) || (scanResult.keys intersect cveKeys).isEmpty()) {
                    return@forEach
                }
                val record = statMap[it.sha256]
                if ((record == null) || record.createdDate.isBefore(it.createdDate)) {
                    val critical = scanResult.getOrDefault(CveOverviewKey.CVE_CRITICAL_COUNT.key, 0).toLong()
                    val high = scanResult.getOrDefault(CveOverviewKey.CVE_HIGH_COUNT.key, 0).toLong()
                    val medium = scanResult.getOrDefault(CveOverviewKey.CVE_MEDIUM_COUNT.key, 0).toLong()
                    val low = scanResult.getOrDefault(CveOverviewKey.CVE_LOW_COUNT.key, 0).toLong()
                    statMap[it.sha256] = LeakStatTemp(it.createdDate, LeakStat(critical, high, medium, low))
                }
            }
        }
        // 聚合所有制品最新记录
        return if (statMap.isEmpty()) LeakStat() else {
            statMap.values.map { it.leakScanResult }.reduce { sum, element ->
                sum.apply {
                    critical += element.critical
                    high += element.high
                    medium += element.medium
                    low += element.low
                }
            }
        }
    }

    private fun calculateScanStat(request: SubtaskInfoRequest, scanStat: ScanStat): ScanStat {
        // 循环分页统计制品扫描和质量规则触发数据
        while (true) {
            val records = archiveSubScanTaskDao.pageBy(request).records
            if (records.isEmpty()) {
                return scanStat.apply { calculateTriggerRate() }
            }
            request.pageNumber ++
            scanStat.scanCount += records.size
            records.forEach {
                val index = ChronoUnit.DAYS.between(
                    request.startDateTime!!.toLocalDate(),
                    it.createdDate.toLocalDate()
                ).toInt()
                val detail = scanStat.dailyStatisticsDetails[index]
                detail.scanCount ++
                if (!it.scanQuality.isNullOrEmpty() && it.qualityRedLine == false) {
                    detail.qualityTriggerCount ++
                    scanStat.qualityTriggerCount ++
                }
            }
        }
    }

    private fun buildDetailList(fromDate: LocalDate, toDate: LocalDate): List<ScanDetail> {
        var current = fromDate
        val days = ChronoUnit.DAYS.between(fromDate, toDate).toInt() + 1
        val detailList = mutableListOf<ScanDetail>()
        repeat(days) {
            detailList.add(ScanDetail(current))
            current = current.plusDays(1)
        }
        return detailList
    }

    companion object {
        private const val PAGE_SIZE = 1000
    }
}
