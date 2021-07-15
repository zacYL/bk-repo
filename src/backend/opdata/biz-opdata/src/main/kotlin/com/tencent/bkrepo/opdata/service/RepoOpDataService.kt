package com.tencent.bkrepo.opdata.service

import com.tencent.bkrepo.opdata.pojo.ArtifactMetricsData
import com.tencent.bkrepo.opdata.pojo.RepoCapacityList
import com.tencent.bkrepo.opdata.pojo.RepoTypeSum
import com.tencent.bkrepo.opdata.pojo.SortType
import com.tencent.bkrepo.opdata.pojo.response.RepoCapacityData
import com.tencent.bkrepo.opdata.pojo.response.RepoVisitData
import com.tencent.bkrepo.repository.pojo.bksoftware.DownloadMetric
import com.tencent.bkrepo.repository.pojo.bksoftware.UploadMetric

interface RepoOpDataService {
    fun repoType(projectId: String?): RepoTypeSum

    fun repoCapacity(projectId: String?, repoName: String?, limit: Int, sort: SortType): RepoCapacityList

    fun repoCapacityData(projectId: String?, repoName: String?): List<RepoCapacityData>

    fun repoVisitData(projectId: String?, repoName: String?): List<RepoVisitData>

    fun repos(projectId: String?): Int

    fun sortByDownload(projectId: String?, repoName: String?): List<ArtifactMetricsData>

    fun downSum(projectId: String?, repoName: String?): Long

    fun downloadsByDay(projectId: String?, repoName: String?, days: Long?): DownloadMetric

    fun uploadsByDay(projectId: String?, repoName: String?, days: Long?): UploadMetric
}
