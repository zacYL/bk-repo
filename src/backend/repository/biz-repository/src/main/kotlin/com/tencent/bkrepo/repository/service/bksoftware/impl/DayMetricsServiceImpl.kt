package com.tencent.bkrepo.repository.service.bksoftware.impl

import com.tencent.bkrepo.repository.dao.DayMetricDao
import com.tencent.bkrepo.repository.model.TDayMetric
import com.tencent.bkrepo.repository.pojo.log.OperateType
import com.tencent.bkrepo.repository.pojo.bksoftware.DayMetricsData
import com.tencent.bkrepo.repository.pojo.bksoftware.DayMetricRequest
import com.tencent.bkrepo.repository.pojo.bksoftware.DayMetricSum
import com.tencent.bkrepo.repository.service.bksoftware.DayMetricService
import org.springframework.data.domain.Sort
import org.springframework.data.mongodb.core.query.Criteria
import org.springframework.data.mongodb.core.query.Query
import org.springframework.data.mongodb.core.query.Update
import org.springframework.data.mongodb.core.query.isEqualTo
import org.springframework.stereotype.Service
import java.time.LocalDate

@Service
class DayMetricsServiceImpl(
    private val dayMetricDao: DayMetricDao
) : DayMetricService {

    override fun list(
        projectId: String?,
        repoName: String?,
        days: Long,
        type: Array<OperateType>
    ): List<DayMetricsData?> {
        val data = LocalDate.now().minusDays(days)
        val query = Query(
            Criteria.where(TDayMetric::day.name).gte(data)
                .and(TDayMetric::operateType.name).`in`(type)
        )
            .limit(days.toInt())
            .with(Sort.by(TDayMetric::day.name).descending())
        projectId?.let { query.addCriteria(Criteria.where(TDayMetric::projectId.name).`is`(projectId)) }
        repoName?.let { query.addCriteria(Criteria.where(TDayMetric::repoName.name).`is`(repoName)) }
        val list = dayMetricDao.find(query, TDayMetric::class.java)
        return mergeByDay(days, projectId, repoName, list)
    }

    override fun add(dayMetricRequest: DayMetricRequest) {
        val query = Query(
            Criteria.where(TDayMetric::day.name).isEqualTo(dayMetricRequest.day)
                .and(TDayMetric::projectId.name).isEqualTo(dayMetricRequest.projectId)
                .and(TDayMetric::repoName.name).isEqualTo(dayMetricRequest.repoName)
                .and(TDayMetric::operateType.name).isEqualTo(dayMetricRequest.type)
        )
        val tDayMetric = dayMetricDao.findOne(query, TDayMetric::class.java)
        if (tDayMetric == null) {
            dayMetricDao.insert(
                TDayMetric(
                    day = dayMetricRequest.day,
                    projectId = dayMetricRequest.projectId,
                    repoName = dayMetricRequest.repoName,
                    count = 1L,
                    operateType = dayMetricRequest.type
                )
            )
        } else {
            val update = Update().inc(TDayMetric::count.name, 1)
            dayMetricDao.upsert(query, update)
        }
    }

    private fun mergeByDay(
        days: Long,
        projectId: String?,
        repoName: String?,
        tDayMetricList: List<TDayMetric?>
    ): List<DayMetricsData> {
        val map = mutableMapOf<LocalDate, DayMetricSum>()
        val list = mutableListOf<DayMetricsData>()
        for (dayMetrics in tDayMetricList) {
            dayMetrics ?: continue
            val dayMetricsSum = map[dayMetrics.day]
            if (dayMetricsSum == null) {
                map[dayMetrics.day] = DayMetricSum(
                    projectId,
                    repoName,
                    dayMetrics.count
                )
            } else {
                dayMetricsSum.count += dayMetrics.count
            }
        }
        val continueMap = continuousDayMap(days, map).toSortedMap(compareBy { it })
        for (key in continueMap.entries) {
            list.add(
                DayMetricsData(
                    time = key.key,
                    projectId = key.value.projectId,
                    repoName = key.value.repoName,
                    count = key.value.count
                )
            )
        }
        return list
    }

    private fun continuousDayMap(
        days: Long,
        map: MutableMap<LocalDate,
            DayMetricSum>
    ): Map<LocalDate, DayMetricSum> {
        val today = LocalDate.now()
        val startDate = today.minusDays(days)
        for (i in 0..days) {
            val date = startDate.plusDays(i)
            if (map[date] == null) {
                map[date] = DayMetricSum(
                    projectId = null,
                    repoName = null,
                    count = 0L
                )
            }
        }
        return map
    }
}
