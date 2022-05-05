package com.tencent.bkrepo.repository.service.project.impl

import com.tencent.bkrepo.repository.dao.PackageDownloadsDao
import com.tencent.bkrepo.repository.dao.PackageUploadsDao
import com.tencent.bkrepo.repository.pojo.metric.PackageDownloadCount
import com.tencent.bkrepo.repository.pojo.project.DayDetail
import com.tencent.bkrepo.repository.pojo.project.ProjectStatisticsSummary
import com.tencent.bkrepo.repository.service.project.ProjectStatisticsService
import com.tencent.bkrepo.repository.util.PackageQueryHelper
import org.springframework.stereotype.Service
import java.time.LocalDate
import java.time.temporal.ChronoUnit

@Service
class ProjectStatisticsServiceImpl(
    private val packageDownloadsDao: PackageDownloadsDao,
    private val packageUploadsDao: PackageUploadsDao
) : ProjectStatisticsService {

    override fun querySummary(projectId: String, fromDate: LocalDate, toDate: LocalDate): ProjectStatisticsSummary {
        val downloadedUsers = mutableSetOf<String>()
        val uploadedUsers = mutableSetOf<String>()
        val daysDetail = buildDayDetailList(fromDate, toDate)
        var downloadCountSum = 0L
        var uploadCountSum = 0L
        // 查询下载相关数据
        val downloadRecordQuery = PackageQueryHelper.recordQuery(projectId, fromDate, toDate)
        packageDownloadsDao.find(downloadRecordQuery).forEach {
            downloadedUsers.addAll(it.users.orEmpty())
            val downloadDate = LocalDate.parse(it.date)
            val index = ChronoUnit.DAYS.between(fromDate, downloadDate).toInt()
            daysDetail[index].downloadCount += it.count
            downloadCountSum += it.count
        }
        // 查询上传相关数据
        val uploadRecordQuery = PackageQueryHelper.recordQuery(
            projectId = projectId,
            fromDate = fromDate,
            toDate = toDate
        )
        packageUploadsDao.find(uploadRecordQuery).forEach {
            uploadedUsers.add(it.userId)
            val uploadDate = LocalDate.parse(it.date)
            val index = ChronoUnit.DAYS.between(fromDate, uploadDate).toInt()
            daysDetail[index].uploadCount += 1
            uploadCountSum += 1
        }

        return ProjectStatisticsSummary(
            uploadCountSum, downloadCountSum,
            uploadedUsers.size.toLong(), downloadedUsers.size.toLong(),
            daysDetail
        )
    }

    override fun queryPackageDownloadRank(
        projectId: String,
        fromDate: LocalDate,
        toDate: LocalDate
    ): List<PackageDownloadCount> {
        val downloadRecordQuery = PackageQueryHelper.recordQuery(projectId, fromDate, toDate)
        val downloadRecord = packageDownloadsDao.find(downloadRecordQuery)
        // 查询结果分组统计并构建结果对象
        return downloadRecord.groupingBy { it.name }
            .aggregate { key, accumulator: PackageDownloadCount?, element, first ->
                if (first)
                    PackageDownloadCount(key, element.count)
                else
                    accumulator?.apply { downloadCount += element.count }
            }.values
            .filterNotNull()
            .sortedByDescending { it.downloadCount }
    }

    private fun buildDayDetailList(fromDate: LocalDate, toDate: LocalDate): List<DayDetail> {
        var current = fromDate
        val days = ChronoUnit.DAYS.between(fromDate, toDate).toInt() + 1
        val dayDetailList = mutableListOf<DayDetail>()
        repeat(days) {
            dayDetailList.add(DayDetail(current, 0, 0))
            current = current.plusDays(1)
        }
        return dayDetailList
    }
}
