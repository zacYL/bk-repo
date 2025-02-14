package com.tencent.bkrepo.repository.service.project

import com.tencent.bkrepo.repository.pojo.metric.NodeDownloadCount
import com.tencent.bkrepo.repository.pojo.metric.PackageDownloadCount
import com.tencent.bkrepo.repository.pojo.node.NodeStatisticsSummary
import com.tencent.bkrepo.repository.pojo.project.ProjectStatisticsSummary
import java.time.LocalDate

interface ProjectStatisticsService {

    /**
     * 查询项目依赖包统计数据总览
     *
     * @param projectId 项目id
     * @param fromDate 查询起始日期（包含）
     * @param toDate 查询结束日期（包含）
     */
    fun queryVersionSummary(projectId: String, fromDate: LocalDate, toDate: LocalDate): ProjectStatisticsSummary

    /**
     * 查询项目依赖包下载量排行
     *
     * @param projectId 项目id
     * @param fromDate 查询起始日期（包含）
     * @param toDate 查询结束日期（包含）
     */
    fun queryPackageDownloadRank(projectId: String, fromDate: LocalDate, toDate: LocalDate): List<PackageDownloadCount>

    /**
     * 查询项目通用文件统计数据总览
     *
     * @param projectId 项目id
     * @param fromDate 查询起始日期（包含）
     * @param toDate 查询结束日期（包含）
     */
    fun queryNodeSummary(projectId: String, fromDate: LocalDate, toDate: LocalDate): NodeStatisticsSummary

    /**
     * 查询项目通用文件下载量排行
     *
     * @param projectId 项目id
     * @param fromDate 查询起始日期（包含）
     * @param toDate 查询结束日期（包含）
     */
    fun queryNodeDownloadRank(projectId: String, fromDate: LocalDate, toDate: LocalDate): List<NodeDownloadCount>
}
