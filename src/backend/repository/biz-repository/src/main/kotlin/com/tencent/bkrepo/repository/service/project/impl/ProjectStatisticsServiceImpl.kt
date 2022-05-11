package com.tencent.bkrepo.repository.service.project.impl

import com.tencent.bkrepo.common.artifact.event.base.EventType
import com.tencent.bkrepo.common.mongo.dao.util.Pages
import com.tencent.bkrepo.common.operate.service.dao.OperateLogDao
import com.tencent.bkrepo.common.operate.service.model.TOperateLog
import com.tencent.bkrepo.repository.dao.PackageDownloadsDao
import com.tencent.bkrepo.repository.dao.PackageUploadsDao
import com.tencent.bkrepo.repository.pojo.metric.DayDetail
import com.tencent.bkrepo.repository.pojo.metric.NodeDownloadCount
import com.tencent.bkrepo.repository.pojo.metric.PackageDownloadCount
import com.tencent.bkrepo.repository.pojo.node.NodeStatisticsSummary
import com.tencent.bkrepo.repository.pojo.project.ProjectStatisticsSummary
import com.tencent.bkrepo.repository.service.project.ProjectStatisticsService
import com.tencent.bkrepo.repository.util.PackageQueryHelper
import org.springframework.data.mongodb.core.query.Criteria
import org.springframework.data.mongodb.core.query.Query
import org.springframework.stereotype.Service
import java.time.LocalDate
import java.time.temporal.ChronoUnit

@Service
class ProjectStatisticsServiceImpl(
    private val packageDownloadsDao: PackageDownloadsDao,
    private val packageUploadsDao: PackageUploadsDao,
    private val operateLogDao: OperateLogDao
) : ProjectStatisticsService {

    override fun queryVersionSummary(
        projectId: String,
        fromDate: LocalDate,
        toDate: LocalDate
    ): ProjectStatisticsSummary {
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

    override fun queryNodeSummary(
        projectId: String,
        fromDate: LocalDate,
        toDate: LocalDate
    ): NodeStatisticsSummary {
        // 初始化数据存储容器
        val downloadedUsers = mutableSetOf<String>()
        val uploadedUsers = mutableSetOf<String>()
        val daysDetail = buildDayDetailList(fromDate, toDate)
        var downloadCountSum = 0L
        var uploadCountSum = 0L

        // 查询上传和下载数据并统计
        val logQuery = nodeOperateLogQuery(projectId, "SUMMARY", fromDate, toDate)
        val pageRequest = Pages.ofRequest(1, MAX_QUERY_COUNT)
        operateLogDao.find(logQuery.with(pageRequest)).forEach {
            val day = daysDetail[ChronoUnit.DAYS.between(fromDate, it.createdDate.toLocalDate()).toInt()]
            if (it.type == EventType.NODE_DOWNLOADED) {
                downloadedUsers.add(it.userId)
                day.downloadCount ++
                downloadCountSum ++
            } else {
                uploadedUsers.add(it.userId)
                day.uploadCount ++
                uploadCountSum ++
            }
        }

        return NodeStatisticsSummary(
            uploadCountSum, downloadCountSum,
            uploadedUsers.size.toLong(), downloadedUsers.size.toLong(),
            daysDetail
        )
    }

    override fun queryNodeDownloadRank(
        projectId: String,
        fromDate: LocalDate,
        toDate: LocalDate
    ): List<NodeDownloadCount> {
        val logQuery = nodeOperateLogQuery(projectId, "DOWNLOAD", fromDate, toDate)
        val pageRequest = Pages.ofRequest(1, MAX_QUERY_COUNT)
        val downloadLogs = operateLogDao.find(logQuery.with(pageRequest))
        val result = mutableListOf<NodeDownloadCount>()
        downloadLogs.groupBy { it.resourceKey }.forEach {
            val element = it.value.first()
            val fullPath = element.resourceKey
            val name = fullPath.split("/").last()
            result.add(NodeDownloadCount(element.repoName.toString(), fullPath, name, it.value.size.toLong()))
        }
        return result.sortedByDescending { it.downloadCount }
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

    private fun nodeOperateLogQuery(
        projectId: String,
        type: String,
        startTime: LocalDate,
        endTime: LocalDate
    ): Query {
        val criteria = Criteria
            .where(TOperateLog::type.name).`in`(getEventTypeList(type))
            .and(TOperateLog::projectId.name).`is`(projectId)

        val localStartTime = startTime.atStartOfDay()
        val localEndTime = endTime.plusDays(1).atStartOfDay()

        criteria.and(TOperateLog::createdDate.name).gte(localStartTime).lt(localEndTime)
        return Query(criteria)
    }

    private fun getEventTypeList(type: String): List<EventType> {
        return when (type) {
            "SUMMARY" -> listOf(EventType.NODE_CREATED, EventType.NODE_DOWNLOADED)
            "DOWNLOAD" -> listOf(EventType.NODE_DOWNLOADED)
            else -> listOf()
        }
    }

    companion object {
        private const val MAX_QUERY_COUNT = 9999999
    }
}
