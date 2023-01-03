package com.tencent.bkrepo.repository.cpack.service.impl

import com.tencent.bkrepo.common.api.pojo.Page
import com.tencent.bkrepo.common.mongo.dao.util.Pages
import com.tencent.bkrepo.repository.cpack.service.PackageVersionDependentsService
import com.tencent.bkrepo.repository.dao.PackageVersionDependentsDao
import com.tencent.bkrepo.repository.model.TPackageVersionDependents
import com.tencent.bkrepo.repository.pojo.dependent.PackageVersionDependentsRelation
import com.tencent.bkrepo.repository.pojo.dependent.PackageVersionDependentsRequest
import org.springframework.data.mongodb.core.query.Query
import org.springframework.data.mongodb.core.query.Update
import org.springframework.data.mongodb.core.query.and
import org.springframework.data.mongodb.core.query.isEqualTo
import org.springframework.data.mongodb.core.query.where
import org.springframework.stereotype.Service
import java.util.regex.Pattern

@Service
class PackageVersionDependentsServiceImpl(
    private val packageVersionDependentsDao: PackageVersionDependentsDao,
) : PackageVersionDependentsService {
    override fun insert(relation: PackageVersionDependentsRelation): Boolean {
        val query = Query.query(
            where(TPackageVersionDependents::projectId).isEqualTo(relation.projectId)
                .and(TPackageVersionDependents::repoName).isEqualTo(relation.repoName)
                .and(TPackageVersionDependents::packageKey).isEqualTo(relation.packageKey)
                .and(TPackageVersionDependents::version).isEqualTo(relation.version)
        )
        val update = Update.update(
            TPackageVersionDependents::dependents.name,
            relation.dependencies
        )
        packageVersionDependentsDao.upsert(query, update)
        return true
    }

    override fun delete(request: PackageVersionDependentsRequest): Boolean {
        val query = Query.query(
            where(TPackageVersionDependents::projectId).isEqualTo(request.projectId)
                .and(TPackageVersionDependents::repoName).isEqualTo(request.repoName)
                .and(TPackageVersionDependents::packageKey).isEqualTo(request.packageKey)
                .and(TPackageVersionDependents::version).isEqualTo(request.version)
        )
        return packageVersionDependentsDao.remove(query).deletedCount == 1L
    }

    override fun get(request: PackageVersionDependentsRequest): Set<String> {
        val query = Query.query(
            where(TPackageVersionDependents::projectId).isEqualTo(request.projectId)
                .and(TPackageVersionDependents::repoName).isEqualTo(request.repoName)
                .and(TPackageVersionDependents::packageKey).isEqualTo(request.packageKey)
                .and(TPackageVersionDependents::version).isEqualTo(request.version)
        )
        return packageVersionDependentsDao.findOne(query)?.dependents ?: emptySet()
    }

    override fun dependenciesReverse(
        searchStr: String,
        projectId: String?,
        repoName: String?,
        pageNumber: Int,
        pageSize: Int
    ): Page<PackageVersionDependentsRelation> {
        val query = Query.query(
            // 搜索字符串添加 `:` 是为了确保版本号的精确匹配，参考如下例子
            // com.example:test:1.0 与 com.example:test:1.0.1，如果不加 `:` 则会匹配到后者
            where(TPackageVersionDependents::dependents).`in`(Pattern.compile("^$searchStr:"))
        )
        projectId?.let { query.addCriteria(where(TPackageVersionDependents::projectId).isEqualTo(it)) }
        repoName?.let { query.addCriteria(where(TPackageVersionDependents::repoName).isEqualTo(it)) }
        val totalRecords = packageVersionDependentsDao.count(query)
        query.with(Pages.ofRequest(pageNumber, pageSize))
            .fields().include(
                TPackageVersionDependents::projectId.name,
                TPackageVersionDependents::repoName.name,
                TPackageVersionDependents::packageKey.name,
                TPackageVersionDependents::version.name
            )
        val packageVersionDependents = packageVersionDependentsDao.find(query)
        return Pages.ofResponse(
            pageRequest = Pages.ofRequest(pageNumber, pageSize),
            totalRecords = totalRecords,
            records = packageVersionDependents.map {
                PackageVersionDependentsRelation(
                    projectId = it.projectId,
                    repoName = it.repoName,
                    packageKey = it.packageKey,
                    version = it.version,
                    dependencies = it.dependents
                )
            }
        )
    }
}
