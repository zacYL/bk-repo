package com.tencent.bkrepo.opdata.service

import com.tencent.bkrepo.opdata.pojo.ArtifactMetricsData
import com.tencent.bkrepo.opdata.pojo.RepoCapacityList
import com.tencent.bkrepo.opdata.pojo.RepoTypeSum
import com.tencent.bkrepo.opdata.pojo.SortType

interface RepoOpDataService {
    fun repoType(projectId: String?): RepoTypeSum

    fun repoCapacity(projectId: String?, repoName: String?, limit: Int, sort: SortType): RepoCapacityList

    fun repos(projectId: String?): Int

    fun sortByDownload(projectId: String?, repoName: String?): List<ArtifactMetricsData>

    fun downSum(projectId: String?, repoName: String?): Long
}
