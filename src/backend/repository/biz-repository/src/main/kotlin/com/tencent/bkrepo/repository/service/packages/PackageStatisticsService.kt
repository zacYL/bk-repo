package com.tencent.bkrepo.repository.service.packages

import com.tencent.bkrepo.repository.pojo.bksoftware.PackageOverviewResponse
import com.tencent.bkrepo.repository.pojo.metric.PackageDetail
import java.time.LocalDateTime

interface PackageStatisticsService {

    /**
     * 软件源包搜索总览
     */
    fun packageOverview(repoType: String, projectId: String?, packageName: String?): PackageOverviewResponse

    /**
     * 制品总数
     */
    fun packageTotal(projectId: String?, repoName: String?): Long

    /**
     * 制品下载总量
     */
    fun packageDownloadSum(projectId: String?, repoName: String?): Long

    /**
     * 查询指定时间之后有更新的包
     */
    fun packageModifiedLimitByTime(time: LocalDateTime, pageNumber: Int, pageSize: Int): List<PackageDetail?>
}
