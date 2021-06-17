package com.tencent.bkrepo.opdata.service.impl

import com.tencent.bkrepo.common.artifact.pojo.RepositoryType
import com.tencent.bkrepo.opdata.pojo.*
import com.tencent.bkrepo.opdata.service.RepoOpDataService
import com.tencent.bkrepo.repository.api.NodeClient
import com.tencent.bkrepo.repository.api.PackageClient
import com.tencent.bkrepo.repository.api.ProjectClient
import com.tencent.bkrepo.repository.api.RepositoryClient
import com.tencent.bkrepo.repository.pojo.metric.PackageDetail
import org.springframework.stereotype.Service
import java.lang.StringBuilder

@Service
class RepoOpDataServiceImpl(
    private val repositoryClient: RepositoryClient,
    private val nodeClient: NodeClient,
    private val projectClient: ProjectClient,
    private val packageClient: PackageClient
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
                val temp = "000${((type.value.num * 10000) / repoSum)}"
                val percent = temp.substring(temp.length - 4, temp.length).let {
                    StringBuilder(it).insert(2, ".").toString()
                }.removePrefix("0")
                type.value.percent = "$percent%"
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

    override fun repoCapacity(projectId: String?, repoName: String?, limit: Int, sort: SortType): RepoCapacityList {
        val repos = repositoryClient.allRepos(projectId, repoName).data ?: return RepoCapacityList(0, null)
        val repoCapacityList = mutableListOf<RepoCapacityDetail>()
        for (repo in repos) {
            repo ?: continue
            if (repo.type == RepositoryType.GENERIC) continue
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
        repoCapacityList.subList(0, limit)
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
}