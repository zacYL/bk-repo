package com.tencent.bkrepo.common.metadata.service.packages.impl

import com.tencent.bkrepo.auth.api.ServicePermissionClient
import com.tencent.bkrepo.common.api.util.EscapeUtils
import com.tencent.bkrepo.common.metadata.condition.SyncCondition
import com.tencent.bkrepo.common.metadata.dao.packages.PackageDao
import com.tencent.bkrepo.common.metadata.dao.packages.PackageVersionDao
import com.tencent.bkrepo.common.metadata.model.TPackage
import com.tencent.bkrepo.common.metadata.model.TPackageVersion
import com.tencent.bkrepo.repository.pojo.software.CountResult
import com.tencent.bkrepo.repository.pojo.software.ProjectPackageOverview
import com.tencent.bkrepo.common.metadata.service.packages.PackageStatisticsService
import com.tencent.bkrepo.common.metadata.service.repo.RepositoryService
import com.tencent.bkrepo.common.mongo.dao.util.Pages
import com.tencent.bkrepo.common.security.util.SecurityUtils
import com.tencent.bkrepo.repository.pojo.metric.PackageDetail
import org.springframework.context.annotation.Conditional
import org.springframework.data.mongodb.core.aggregation.Aggregation
import org.springframework.data.mongodb.core.query.Criteria
import org.springframework.data.mongodb.core.query.Query
import org.springframework.stereotype.Service
import java.time.LocalDateTime
import java.util.Locale

@Service
@Conditional(SyncCondition::class)
class PackageStatisticsServiceImpl(
    private val packageDao: PackageDao,
    private val packageVersionDao: PackageVersionDao,
    private val repositoryService: RepositoryService,
    private val servicePermissionClient: ServicePermissionClient
) : PackageStatisticsService {
    override fun packageTotal(projectId: String?, repoName: String?): Long {
        val criteria = Criteria()
        projectId?.let { criteria.and(TPackage::projectId.name).`is`(projectId) }
        repoName?.let { criteria.and(TPackage::repoName.name).`is`(repoName) }
        val aggregation = Aggregation.newAggregation(
            TPackage::class.java,
            Aggregation.match(criteria),
            Aggregation.project("versions"),
            Aggregation.group().sum("versions").`as`("count"),
            Aggregation.project("_id", "count")
        )
        val result = packageDao.aggregate(aggregation, CountResult::class.java).mappedResults
        return if (result.isEmpty()) 0 else result[0].count
    }

    override fun packageDownloadSum(projectId: String?, repoName: String?): Long {
        val criteria = Criteria()
        projectId?.let { criteria.and(TPackage::projectId.name).`is`(projectId) }
        repoName?.let { criteria.and(TPackage::repoName.name).`is`(repoName) }
        val aggregation = Aggregation.newAggregation(
            TPackage::class.java,
            Aggregation.match(criteria),
            Aggregation.group().sum("\$downloads").`as`("count"),
            Aggregation.project("_id", "count")
        )
        val result = packageDao.aggregate(aggregation, CountResult::class.java).mappedResults
        return if (result.isEmpty()) 0 else result[0].count
    }

    override fun packageModifiedLimitByTime(
        time: LocalDateTime,
        pageNumber: Int,
        pageSize: Int
    ): List<PackageDetail?> {
        val query = Query(Criteria.where(TPackageVersion::lastModifiedDate.name).gte(time))
        val page = Pages.ofRequest(pageNumber, pageSize)
        return packageVersionDao.find(query.with(page)).map { convert(it) }
    }

    override fun packageOverview(
        repoType: String,
        projectId: String,
        packageName: String?
    ): List<ProjectPackageOverview> {
        val repoNames = servicePermissionClient.listPermissionRepo(
            projectId = projectId,
            userId = SecurityUtils.getUserId(),
            appId = SecurityUtils.getPlatformId(),
            actions = null
        ).data.orEmpty()
        val criteria = Criteria.where(TPackage::type.name).`is`(repoType.uppercase(Locale.getDefault()))
        projectId.let { criteria.and(TPackage::projectId.name).`is`(projectId) }
        repoNames.let { criteria.and(TPackage::repoName.name).`in`(repoNames) }
        packageName?.let {
            val escapedValue = EscapeUtils.escapeRegexExceptWildcard(packageName)
            val regexPattern = escapedValue.replace("*", ".*")
            criteria.and(TPackage::name.name).regex("^$regexPattern$", "i")
        }
        val aggregation = Aggregation.newAggregation(
            TPackage::class.java,
            Aggregation.match(criteria),
            Aggregation.group("\$${TPackage::repoName.name}").count().`as`("count")
        )
        val result = packageDao.aggregate(aggregation, CountResult::class.java).mappedResults
        return transTree(projectId, result)
    }

    private fun transTree(projectId: String, list: List<CountResult>): List<ProjectPackageOverview> {
        val projectSet = mutableSetOf<ProjectPackageOverview>()
        projectSet.add(
            ProjectPackageOverview(
                projectId = projectId,
                repos = mutableSetOf(),
                sum = 0L
            )
        )
        list.map { pojo ->
            val repoOverview = ProjectPackageOverview.RepoPackageOverview(
                repoName = pojo.id,
                repoCategory = repositoryService.getRepoInfo(projectId, pojo.id)?.category,
                packages = pojo.count
            )
            projectSet.first().repos.add(repoOverview)
            projectSet.first().sum += pojo.count
        }
        return projectSet.toList()
    }

    private fun convert(tPackageVersion: TPackageVersion?): PackageDetail? {
        return tPackageVersion?.let {
            val tPackage = packageDao.findById(tPackageVersion.packageId) ?: return null
            PackageDetail(
                projectId = tPackage.projectId,
                repoName = tPackage.repoName,
                packageName = tPackage.name,
                key = tPackage.key,
                type = tPackage.type,
                name = it.name,
                size = it.size,
                downloads = it.downloads,
                lastModifiedDate = it.lastModifiedDate,
                packageId = it.packageId
            )
        }
    }
}
