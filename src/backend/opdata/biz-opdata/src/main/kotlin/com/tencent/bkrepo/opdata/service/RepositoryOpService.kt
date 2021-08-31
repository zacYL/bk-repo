package com.tencent.bkrepo.opdata.service

import com.tencent.bkrepo.opdata.pojo.RepoCapacityList
import com.tencent.bkrepo.opdata.pojo.RepoTypeSum
import com.tencent.bkrepo.opdata.pojo.RepositoryOpUpdateRequest
import com.tencent.bkrepo.opdata.pojo.SortType
import com.tencent.bkrepo.opdata.pojo.response.RepoCapacityData
import com.tencent.bkrepo.opdata.pojo.response.RepoVisitData
import com.tencent.bkrepo.opdata.pojo.response.RepositoryOpResponse
import com.tencent.bkrepo.repository.pojo.bksoftware.DownloadMetric
import com.tencent.bkrepo.repository.pojo.bksoftware.UploadMetric

interface RepositoryOpService {

    fun downloadsByDay(projectId: String?, repoName: String?, days: Long?): DownloadMetric

    fun uploadsByDay(projectId: String?, repoName: String?, days: Long?): UploadMetric

    fun repos(projectId: String?): Int

    fun repoType(projectId: String?): RepoTypeSum

    fun get(projectId: String, repoName: String): RepositoryOpResponse?

    fun update(request: RepositoryOpUpdateRequest): Boolean

    fun repoCapacityData(projectId: String?, repoName: String?): List<RepoCapacityData>

    fun repoVisitData(projectId: String?, repoName: String?): List<RepoVisitData>

    fun packages(projectId: String?, repoName: String?): Long

    fun usedCapacity(projectId: String?, repoName: String?): Long

    fun repoDownloads(projectId: String?, repoName: String?): Long

    fun sortByUsedCapacity(projectId: String?, repoName: String?, limit: Int, sortType: SortType): RepoCapacityList
}
