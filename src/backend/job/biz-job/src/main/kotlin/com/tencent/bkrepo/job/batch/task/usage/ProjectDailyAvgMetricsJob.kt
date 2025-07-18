/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
 *
 * Copyright (C) 2022 Tencent.  All rights reserved.
 *
 * BK-CI 蓝鲸持续集成平台 is licensed under the MIT license.
 *
 * A copy of the MIT License is included in this file.
 *
 *
 * Terms of the MIT License:
 * ---------------------------------------------------
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
 * documentation files (the "Software"), to deal in the Software without restriction, including without limitation the
 * rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to
 * permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of
 * the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT
 * LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN
 * NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
 * SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package com.tencent.bkrepo.job.batch.task.usage

import com.tencent.bkrepo.common.api.constant.StringPool
import com.tencent.bkrepo.job.PROJECT
import com.tencent.bkrepo.job.batch.base.DefaultContextJob
import com.tencent.bkrepo.job.batch.base.JobContext
import com.tencent.bkrepo.job.config.properties.ProjectDailyAvgMetricsJobProperties
import com.tencent.bkrepo.job.pojo.project.TProjectMetricsDailyAvgRecord
import com.tencent.bkrepo.repository.pojo.project.ProjectMetadata
import org.slf4j.LoggerFactory
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.data.mongodb.core.MongoTemplate
import org.springframework.data.mongodb.core.query.Criteria
import org.springframework.data.mongodb.core.query.Query
import org.springframework.data.mongodb.core.query.Update
import org.springframework.data.mongodb.core.query.isEqualTo
import org.springframework.data.mongodb.core.query.where
import org.springframework.stereotype.Component
import java.math.BigDecimal
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/**
 * 根据每日用量采点数据生成每日平均用量
 */
@Component
@EnableConfigurationProperties(ProjectDailyAvgMetricsJobProperties::class)
class ProjectDailyAvgMetricsJob(
    val properties: ProjectDailyAvgMetricsJobProperties,
    private val mongoTemplate: MongoTemplate
) : DefaultContextJob(properties) {

    override fun doStart0(jobContext: JobContext) {
        val days = mutableListOf<LocalDateTime>()
        if (properties.reRunDays.isNotEmpty()) {
            val dateTimeFormatter = DateTimeFormatter.ofPattern("yyyyMMdd")
            properties.reRunDays.forEach {
                days.add(LocalDate.parse(it, dateTimeFormatter).atStartOfDay())
            }
        } else {
            days.add(LocalDate.now().atStartOfDay())
        }
        days.forEach {
            doStoreProjectDailyAvgRecord(it)
        }
    }

    override fun getLockAtMostFor(): Duration = Duration.ofDays(1)

    private fun doStoreProjectDailyAvgRecord(currentDate: LocalDateTime) {
        val yesterday = currentDate.minusDays(1)
        val yesterdayStr = yesterday.format(DateTimeFormatter.ofPattern("yyyyMMdd"))
        logger.info("start to store avg record for $yesterdayStr")
        val criteria = Criteria.where(ProjectMetricsDailyRecord::createdDay.name).isEqualTo(yesterdayStr)
        val query = Query(criteria)
        val data = mongoTemplate.findDistinct(
            query, ProjectMetricsDailyRecord::projectId.name,
            COLLECTION_NAME_PROJECT_METRICS_DAILY_RECORD, String::class.java
        )
        data.forEach {
            val projectCriteria = Criteria.where(PROJECT).isEqualTo(it).andOperator(criteria)
            val projectQuery = Query.query(projectCriteria)
            var capSize = 0L
            var count = 0
            mongoTemplate.find(
                projectQuery, ProjectMetricsDailyRecord::class.java,
                COLLECTION_NAME_PROJECT_METRICS_DAILY_RECORD
            ).forEach {
                capSize += it.capSize
                count++
            }
            handleProjectDailyAvgRecord(
                projectId = it,
                capSize = capSize,
                count = count,
                yesterday = yesterday
            )
        }
    }

    private fun handleProjectDailyAvgRecord(
        projectId: String, capSize: Long, count: Int, yesterday: LocalDateTime
    ) {
        val query = Query(where(ProjectInfo::name).isEqualTo(projectId))
        val projectInfo = mongoTemplate.find(query, ProjectInfo::class.java, COLLECTION_NAME_PROJECT)
            .firstOrNull() ?: return
        val usage = (capSize.toDouble() / (count * 1024 * 1024 * 1024L))
            .toBigDecimal().setScale(2, BigDecimal.ROUND_HALF_UP).toDouble()
        storeDailyAvgRecord(projectInfo, yesterday, usage)
    }

    private fun storeDailyAvgRecord(
        projectInfo: ProjectInfo,
        yesterday: LocalDateTime,
        usage: Double,
    ) {
        val productId = projectInfo.metadata.firstOrNull { it.key == ProjectMetadata.KEY_PRODUCT_ID }?.value as? Int
        val bgId = projectInfo.metadata.firstOrNull { it.key == ProjectMetadata.KEY_BG_ID }?.value as? String
            ?: StringPool.EMPTY
        val enabled = projectInfo.metadata.firstOrNull { it.key == ProjectMetadata.KEY_ENABLED }?.value as? Boolean
            ?: false
        val dailyRecord = TProjectMetricsDailyAvgRecord(
            projectId = projectInfo.name,
            costDate = convertToCostDate(yesterday),
            name = projectInfo.displayName,
            usage = usage,
            bgName = projectInfo.metadata.firstOrNull { it.key == ProjectMetadata.KEY_BG_NAME }?.value as? String
                ?: StringPool.EMPTY,
            flag = covertToFlag(projectInfo.name, bgId, productId, enabled),
            costDateDay = yesterday.format(
                DateTimeFormatter.ofPattern("yyyyMMdd")
            ),
        )
        val query = Query(
            where(TProjectMetricsDailyAvgRecord::projectId).isEqualTo(dailyRecord.projectId)
                .and(TProjectMetricsDailyAvgRecord::costDate.name).isEqualTo(dailyRecord.costDate)
                .and(TProjectMetricsDailyAvgRecord::costDateDay.name).isEqualTo(dailyRecord.costDateDay)
        )
        val update = buildUpdateRecord(dailyRecord)
        mongoTemplate.upsert(query, update, COLLECTION_NAME_PROJECT_METRICS_DAILY_AVG_RECORD)
    }

    private fun buildUpdateRecord(dailyRecord: TProjectMetricsDailyAvgRecord): Update {
        val update = Update()
        update.set(TProjectMetricsDailyAvgRecord::name.name, dailyRecord.name)
        update.set(TProjectMetricsDailyAvgRecord::usage.name, dailyRecord.usage)
        update.set(TProjectMetricsDailyAvgRecord::bgName.name, dailyRecord.bgName)
        update.set(TProjectMetricsDailyAvgRecord::flag.name, dailyRecord.flag)
        return update
    }

    private fun covertToFlag(
        projectId: String,
        bgId: String,
        productId: Int?,
        enabled: Boolean
    ): Boolean {
        val streamReport = if (properties.reportStream) {
            true
        } else {
            !projectId.startsWith(GIT_PROJECT_PREFIX)
        }
        return if (properties.bgIds.isEmpty()) {
            streamReport && bgId.isNotBlank() && productId != null && enabled
        } else {
            streamReport && properties.bgIds.contains(bgId)
                && bgId.isNotBlank() && productId != null && enabled
        }
    }

    private fun convertToCostDate(yesterday: LocalDateTime): String {
        val day = yesterday.dayOfMonth
        val minusMonth = if (day >= properties.monthStartDay) {
            -1
        } else {
            0
        }
        return yesterday.minusMonths(minusMonth.toLong()).format(
            DateTimeFormatter.ofPattern("yyyyMM")
        )
    }

    data class ProjectInfo(
        val name: String,
        val displayName: String,
        val metadata: List<ProjectMetadata> = emptyList(),
    )

    data class ProjectMetricsDailyRecord(
        var projectId: String,
        var capSize: Long,
        var createdDay: String? = null
    )

    companion object {
        private val logger = LoggerFactory.getLogger(ProjectDailyAvgMetricsJob::class.java)
        private const val COLLECTION_NAME_PROJECT = "project"
        private const val COLLECTION_NAME_PROJECT_METRICS_DAILY_RECORD = "project_metrics_daily_record"
        private const val COLLECTION_NAME_PROJECT_METRICS_DAILY_AVG_RECORD = "project_metrics_daily_avg_record"
        private const val GIT_PROJECT_PREFIX = "git_"
    }
}
