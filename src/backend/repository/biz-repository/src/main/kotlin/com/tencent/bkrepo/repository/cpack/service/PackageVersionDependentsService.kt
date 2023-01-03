package com.tencent.bkrepo.repository.cpack.service

import com.tencent.bkrepo.common.api.pojo.Page
import com.tencent.bkrepo.repository.pojo.dependent.PackageVersionDependentsRelation
import com.tencent.bkrepo.repository.pojo.dependent.PackageVersionDependentsRequest

interface PackageVersionDependentsService {
    fun insert(relation: PackageVersionDependentsRelation): Boolean

    fun delete(request: PackageVersionDependentsRequest): Boolean

    fun get(request: PackageVersionDependentsRequest): Set<String>

    fun dependenciesReverse(
        searchStr: String,
        projectId: String?,
        repoName: String?,
        pageNumber: Int,
        pageSize: Int
    ): Page<PackageVersionDependentsRelation>
}
