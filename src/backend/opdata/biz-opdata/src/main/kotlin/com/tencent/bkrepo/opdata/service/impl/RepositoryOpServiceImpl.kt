package com.tencent.bkrepo.opdata.service.impl

import com.tencent.bkrepo.common.artifact.event.base.EventType
import com.tencent.bkrepo.common.artifact.pojo.RepositoryType
import com.tencent.bkrepo.opdata.model.TRepositoryOp
import com.tencent.bkrepo.opdata.pojo.RepoTypeSum
import com.tencent.bkrepo.opdata.pojo.RepoTypeValue
import com.tencent.bkrepo.opdata.pojo.RepoTypeData
import com.tencent.bkrepo.opdata.pojo.RepositoryOpUpdateRequest
import com.tencent.bkrepo.opdata.pojo.RepoCapacityList
import com.tencent.bkrepo.opdata.pojo.RepoCapacityDetail
import com.tencent.bkrepo.opdata.pojo.SortType
import com.tencent.bkrepo.opdata.pojo.response.RepoCapacityData
import com.tencent.bkrepo.opdata.pojo.response.RepoVisitData
import com.tencent.bkrepo.opdata.pojo.response.RepositoryOpResponse
import com.tencent.bkrepo.opdata.repository.RepositoryOpRepository
import com.tencent.bkrepo.opdata.service.DayMetricService
import com.tencent.bkrepo.opdata.service.RepositoryOpService
import com.tencent.bkrepo.repository.api.ProjectClient
import com.tencent.bkrepo.repository.api.RepositoryClient
import com.tencent.bkrepo.repository.pojo.bksoftware.DownloadMetric
import com.tencent.bkrepo.repository.pojo.bksoftware.UploadMetric
import org.springframework.data.mongodb.core.MongoTemplate
import org.springframework.data.mongodb.core.query.Criteria
import org.springframework.data.mongodb.core.query.Query
import org.springframework.data.mongodb.core.query.Update
import org.springframework.stereotype.Service
import java.text.DecimalFormat
import java.time.LocalDateTime

@Service
class RepositoryOpServiceImpl(
    private val repositoryOpRepository: RepositoryOpRepository,
    private val mongoTemplate: MongoTemplate,
    private val repositoryClient: RepositoryClient,
    private val projectClient: ProjectClient,
    private val dayMetricService: DayMetricService
) : RepositoryOpService {

    override fun downloadsByDay(projectId: String?, repoName: String?, days: Long?): DownloadMetric {
        val list = dayMetricService.list(projectId, repoName, days ?: 7L, listOf(EventType.VERSION_DOWNLOAD))
        return DownloadMetric(list)
    }

    override fun uploadsByDay(projectId: String?, repoName: String?, days: Long?): UploadMetric {
        val list = dayMetricService.list(
            projectId, repoName, days ?: 7L, listOf(EventType.VERSION_UPDATED, EventType.VERSION_CREATED)
        )
        return UploadMetric(list)
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

    private fun aggregate(projectId: String?, repoName: String?): List<TRepositoryOp> {
        val repos = repositoryClient.allRepos(projectId, repoName).data ?: return listOf()
        val repoOpList = mutableListOf<TRepositoryOp>()
        for (repo in repos) {
            repo ?: continue
            repositoryOpRepository.findByProjectIdAndRepoName(repo.projectId, repo.name)?.let { repoOpList.add(it) }
        }
        return repoOpList
    }

    override fun get(projectId: String, repoName: String): RepositoryOpResponse? {
        val tRepositoryOp = repositoryOpRepository.findByProjectIdAndRepoName(projectId, repoName)
        return transfer(tRepositoryOp)
    }

    override fun update(request: RepositoryOpUpdateRequest): Boolean {
        val query = Query.query(
            Criteria.where(TRepositoryOp::projectId.name).`is`(request.projectId)
                .and(TRepositoryOp::repoName.name).`is`(request.repoName)
                .and(TRepositoryOp::repoType.name).`is`(request.repoType)
        )
        val update = Update().apply {
            this.set(TRepositoryOp::projectId.name, request.projectId)
            this.set(TRepositoryOp::repoName.name, request.repoName)
            this.set(TRepositoryOp::repoType.name, request.repoType)
            this.set(TRepositoryOp::downloads.name, request.downloads)
            this.set(TRepositoryOp::uploads.name, request.uploads)
            this.set(TRepositoryOp::usedCapacity.name, request.usedCapacity)
            this.set(TRepositoryOp::packages.name, request.packages)
            this.set(TRepositoryOp::visits.name, request.visits)
            this.set(TRepositoryOp::latestModifiedDate.name, LocalDateTime.now())
        }
        val result = mongoTemplate.upsert(query, update, TRepositoryOp::class.java)
        return result.modifiedCount == 1L
    }

    override fun repoCapacityData(projectId: String?, repoName: String?): List<RepoCapacityData> {
        val repoOpList = aggregate(projectId, repoName)
        return repoOpList.map {
            RepoCapacityData(
                projectId = it.projectId,
                repoName = it.repoName,
                repoType = it.repoType,
                capacityLimit = Long.MAX_VALUE,
                usedCapacity = it.usedCapacity
            )
        }
    }

    override fun repoVisitData(projectId: String?, repoName: String?): List<RepoVisitData> {
        val repoOpList = aggregate(projectId, repoName)
        return repoOpList.map {
            RepoVisitData(
                projectId = it.projectId,
                repoName = it.repoName,
                repoType = it.repoType,
                downloads = it.downloads,
                uploads = it.uploads
            )
        }
    }

    override fun packages(projectId: String?, repoName: String?): Long {
        val repoOpList = aggregate(projectId, repoName)
        var packages = 0L
        repoOpList.map { packages += it.packages }
        return packages
    }

    override fun usedCapacity(projectId: String?, repoName: String?): Long {
        val repoOpList = aggregate(projectId, repoName)
        var usedCapacity = 0L
        repoOpList.map { usedCapacity += it.usedCapacity }
        return usedCapacity
    }

    override fun repoDownloads(projectId: String?, repoName: String?): Long {
        val repoOpList = aggregate(projectId, repoName)
        var downloads = 0L
        repoOpList.map { downloads += it.downloads }
        return downloads
    }

    override fun sortByUsedCapacity(projectId: String?, repoName: String?, limit: Int, sortType: SortType):
            RepoCapacityList {
        val criteria = Criteria()
        projectId?.let { criteria.and(TRepositoryOp::projectId.name).`is`(projectId) }
        repoName?.let { criteria.and(TRepositoryOp::repoName.name).`is`(repoName) }
        val query = Query(criteria).limit(limit)
        val result = mongoTemplate.find(query, TRepositoryOp::class.java)
        var count = 0L
        val repoCapacityList = result.map {
            count += it.usedCapacity
            RepoCapacityDetail(
                projectId = it.projectId,
                name = it.repoName,
                usedCapacity = it.usedCapacity
            )
        }
        return RepoCapacityList(
            count = count,
            repos = repoCapacityList
        )
    }

    private fun transfer(tRepositoryOp: TRepositoryOp?): RepositoryOpResponse? {
        tRepositoryOp ?: return null
        return RepositoryOpResponse(
            projectId = tRepositoryOp.projectId,
            repoName = tRepositoryOp.repoName,
            capacity = Long.MAX_VALUE,
            visits = tRepositoryOp.visits,
            downloads = tRepositoryOp.downloads,
            uploads = tRepositoryOp.uploads,
            usedCapacity = tRepositoryOp.usedCapacity,
            packages = tRepositoryOp.packages,
            latestModifiedDate = tRepositoryOp.latestModifiedDate
        )
    }

    companion object {
        val decimalFormat = DecimalFormat("##.00%")
    }
}
