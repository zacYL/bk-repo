package com.tencent.bkrepo.repository.cpack.service

import com.tencent.bkrepo.common.api.pojo.Page
import com.tencent.bkrepo.repository.pojo.dependent.VersionDependentsRelation
import com.tencent.bkrepo.repository.pojo.dependent.VersionDependentsRequest

interface VersionDependentsService {
    fun insert(relation: VersionDependentsRelation): Boolean

    fun delete(request: VersionDependentsRequest): Boolean

    fun get(request: VersionDependentsRequest): Set<String>

    fun dependenciesReverse(
        searchStr: String,
        projectId: String?,
        repoName: String?,
        pageNumber: Int,
        pageSize: Int
    ): Page<VersionDependentsRelation>
}
