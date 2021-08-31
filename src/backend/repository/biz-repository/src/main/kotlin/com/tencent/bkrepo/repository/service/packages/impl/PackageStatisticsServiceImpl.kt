package com.tencent.bkrepo.repository.service.packages.impl

import com.tencent.bkrepo.common.mongo.dao.util.Pages
import com.tencent.bkrepo.common.query.util.MongoEscapeUtils
import com.tencent.bkrepo.repository.dao.PackageDao
import com.tencent.bkrepo.repository.dao.PackageVersionDao
import com.tencent.bkrepo.repository.model.TPackage
import com.tencent.bkrepo.repository.model.TPackageVersion
import com.tencent.bkrepo.repository.pojo.bksoftware.PackageOverviewResponse
import com.tencent.bkrepo.repository.pojo.metric.CountResult
import com.tencent.bkrepo.repository.pojo.metric.PackageDetail
import com.tencent.bkrepo.repository.service.packages.PackageStatisticsService
import org.springframework.data.mongodb.core.aggregation.Aggregation
import org.springframework.data.mongodb.core.query.Criteria
import org.springframework.data.mongodb.core.query.Query
import org.springframework.stereotype.Service
import java.time.LocalDateTime

@Service
class PackageStatisticsServiceImpl(
    private val packageDao: PackageDao,
    private val packageVersionDao: PackageVersionDao
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

    override fun packageOverview(repoType: String, projectId: String?, packageName: String?): PackageOverviewResponse {
        val criteria = Criteria.where(TPackage::type.name).`is`(repoType)
        projectId?.let { criteria.and(TPackage::projectId.name).`is`(projectId) }
        packageName?.let {
            val escapedValue = MongoEscapeUtils.escapeRegexExceptWildcard(packageName)
            val regexPattern = escapedValue.replace("*", ".*")
            criteria.and(TPackage::name.name).regex("^$regexPattern$")
        }
        val aggregation = Aggregation.newAggregation(
            TPackage::class.java,
            Aggregation.match(criteria),
            Aggregation.group("\$repoName").count().`as`("count")
        )
        val result = packageDao.aggregate(aggregation, CountResult::class.java).mappedResults
        var sum = 0L
        val list = mutableListOf<PackageOverviewResponse.RepoPackageOverview>()
        result.map {
            list.add(
                PackageOverviewResponse.RepoPackageOverview(
                    repoName = it.id!!,
                    packages = it.count
                )
            )
            sum += it.count
        }
        return PackageOverviewResponse(list = list, sum = sum)
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
