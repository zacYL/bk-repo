package com.tencent.bkrepo.opdata.service.impl

import com.tencent.bkrepo.common.artifact.pojo.RepositoryType
import com.tencent.bkrepo.opdata.pojo.RepoTypeSum
import com.tencent.bkrepo.opdata.pojo.RepoTypeData
import com.tencent.bkrepo.opdata.pojo.RepoTypeValue
import com.tencent.bkrepo.opdata.pojo.RepoCapacityDetail
import com.tencent.bkrepo.opdata.pojo.SortType
import com.tencent.bkrepo.opdata.pojo.RepoCapacityList
import com.tencent.bkrepo.opdata.pojo.ArtifactMetricsData
import com.tencent.bkrepo.opdata.pojo.response.RepoCapacityData
import com.tencent.bkrepo.opdata.pojo.response.RepoVisitData
import com.tencent.bkrepo.opdata.service.RepoOpDataService
import com.tencent.bkrepo.repository.api.RepositoryClient
import com.tencent.bkrepo.repository.api.NodeClient
import com.tencent.bkrepo.repository.api.ProjectClient
import com.tencent.bkrepo.repository.api.PackageClient
import com.tencent.bkrepo.repository.api.OperateLogClient
import com.tencent.bkrepo.repository.api.DayMetricClient
import com.tencent.bkrepo.repository.pojo.bksoftware.DownloadMetric
import com.tencent.bkrepo.repository.pojo.bksoftware.UploadMetric
import com.tencent.bkrepo.repository.pojo.metric.PackageDetail
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.text.DecimalFormat

@Service
class RepoOpDataServiceImpl(
    private val repositoryClient: RepositoryClient,
    private val nodeClient: NodeClient,
    private val projectClient: ProjectClient,
    private val packageClient: PackageClient,
    private val operateLogClient: OperateLogClient,
    private val dayMetricClient: DayMetricClient
) : RepoOpDataService {

    override fun repoType(projectId: String?): RepoTypeSum {
        val map = mutableMapOf<RepositoryType, RepoTypeValue>()
        val set = mutableSetOf<RepoTypeData>()
        val repoList = repositoryClient.allRepos(projectId, null).data ?: mutableListOf()
        if (repoList.isNotEmpty()) {
            val repoSum = repoList.size
            for (repo in repoList) {
                repo ?: continue
                val repoTypeSum = map[repo.type]
                if (repoTypeSum == null) {
                    map[repo.type] = RepoTypeValue(1L, "0")
                } else {
                    repoTypeSum.num += 1
                }
            }

            for (type in map.entries) {
                type.value.percent = decimalFormat.format((type.value.num * 1.0) / (repoSum * 1.0))
                set.add(
                    RepoTypeData(
                        type = type.key,
                        count = type.value.num,
                        percent = type.value.percent
                    )
                )
            }
        }
        return RepoTypeSum(set)
    }

    override fun repoCapacityData(
        projectId: String?,
        repoName: String?
    ): List<RepoCapacityData> {
        val repos = repositoryClient.allRepos(projectId, repoName).data ?: return listOf()
        val repoCapacityDataList = mutableListOf<RepoCapacityData>()
        for (repo in repos) {
            if (repo == null || repo.type == RepositoryType.GENERIC) continue
            val usedCapacity = nodeClient.capacity(repo.projectId, repo.name).data ?: 0L
            repoCapacityDataList.add(
                RepoCapacityData(
                    repo.projectId,
                    repo.name,
                    repo.type,
                    capacityLimit = Long.MAX_VALUE,
                    usedCapacity = usedCapacity
                )
            )
        }
        return repoCapacityDataList
    }

    override fun repoVisitData(projectId: String?, repoName: String?): List<RepoVisitData> {
        val repos = repositoryClient.allRepos(projectId, repoName).data ?: return listOf()
        val repoVisitDataList = mutableListOf<RepoVisitData>()
        for (repo in repos) {
            if (repo == null || repo.type == RepositoryType.GENERIC) continue
            repoVisitDataList.add(
                RepoVisitData(
                    repo.projectId,
                    repo.name,
                    repo.type,
                    downloads = operateLogClient.downloads(repo.projectId, repo.name, null).data ?: 0L,
                    uploads = operateLogClient.uploads(repo.projectId, repo.name, null).data ?: 0L
                )
            )
        }
        return repoVisitDataList
    }

    override fun repoCapacity(projectId: String?, repoName: String?, limit: Int, sort: SortType): RepoCapacityList {
        val repos = repositoryClient.allRepos(projectId, repoName).data ?: return RepoCapacityList(0, null)
        val repoCapacityList = mutableListOf<RepoCapacityDetail>()
        for (repo in repos) {
            if (repo == null || repo.type == RepositoryType.GENERIC) continue
            val capacity = nodeClient.capacity(repo.projectId, repo.name).data ?: 0L
            repoCapacityList.add(
                RepoCapacityDetail(
                    repo.projectId,
                    repo.name,
                    used = capacity
                )
            )
        }
        val count = repoCapacityList.fold<RepoCapacityDetail, Long>(0) { acc, v -> acc + v.used }
        when (sort) {
            SortType.USED -> repoCapacityList.sortByDescending { it.used }
            SortType.RESIDUAL -> repoCapacityList.sortBy { (it.limit.minus(it.used)) }
        }
        val targetLimit = if (limit >= repoCapacityList.size) repoCapacityList.size else limit
        repoCapacityList.subList(0, targetLimit)
        return RepoCapacityList(
            count = count,
            repos = repoCapacityList
        )
    }

    override fun repos(projectId: String?): Int {
        return if (projectId != null) {
            repositoryClient.listRepo(projectId).data?.size ?: 0
        } else {
            var result = 0
            val projects = projectClient.listProject().data ?: return 0
            for (project in projects) {
                result += repositoryClient.listRepo(project.name).data?.size ?: 0
            }
            result
        }
    }

    override fun sortByDownload(projectId: String?, repoName: String?): List<ArtifactMetricsData> {
        val list = packageClient.sortByDown(projectId, repoName).data ?: listOf()
        return list.map { transferPackageDetail(it) }
    }

    override fun downloadsByDay(projectId: String?, repoName: String?, days: Long?): DownloadMetric {
        return dayMetricClient.listByDownload(projectId, repoName, days).data!!
    }

    override fun uploadsByDay(projectId: String?, repoName: String?, days: Long?): UploadMetric {
        return dayMetricClient.listByUpload(projectId, repoName, days).data!!
    }

    override fun downSum(projectId: String?, repoName: String?): Long {
        return packageClient.downloads(projectId, repoName).data ?: 0L
    }

    private fun transferPackageDetail(packageDetail: PackageDetail): ArtifactMetricsData {
        return ArtifactMetricsData(
            name = packageDetail.packageName,
            projectId = packageDetail.projectId,
            repoName = packageDetail.repoName,
            repoType = packageDetail.type,
            packageName = packageDetail.packageName,
            packageVersion = packageDetail.name,
            count = packageDetail.downloads,
            size = packageDetail.size,
            packageKey = packageDetail.key
        )
    }

    companion object {
        private val logger: Logger = LoggerFactory.getLogger(RepoOpDataServiceImpl::class.java)
        val decimalFormat = DecimalFormat("##.00%")
    }
}
