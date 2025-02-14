package com.tencent.bkrepo.common.metadata.service.packages

import com.tencent.bkrepo.repository.pojo.metric.PackageDetail
import com.tencent.bkrepo.repository.pojo.software.ProjectPackageOverview
import java.time.LocalDateTime

interface PackageStatisticsService {

    /**
     * 包搜索总览
     */
    fun packageOverview(repoType: String, projectId: String, packageName: String?): List<ProjectPackageOverview>

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
