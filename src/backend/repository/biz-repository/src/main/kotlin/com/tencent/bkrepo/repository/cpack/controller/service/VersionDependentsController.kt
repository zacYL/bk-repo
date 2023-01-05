package com.tencent.bkrepo.repository.cpack.controller.service

import com.tencent.bkrepo.common.api.pojo.Page
import com.tencent.bkrepo.common.api.pojo.Response
import com.tencent.bkrepo.common.service.util.ResponseBuilder
import com.tencent.bkrepo.repository.api.VersionDependentsClient
import com.tencent.bkrepo.repository.cpack.service.VersionDependentsService
import com.tencent.bkrepo.repository.pojo.dependent.VersionDependentsRelation
import com.tencent.bkrepo.repository.pojo.dependent.VersionDependentsRequest
import org.springframework.web.bind.annotation.RestController

@RestController
class VersionDependentsController(
    private val packageVersionDependentsService: VersionDependentsService
) : VersionDependentsClient {
    override fun insert(relation: VersionDependentsRelation): Response<Boolean> {
        return ResponseBuilder.success(packageVersionDependentsService.insert(relation))
    }

    override fun delete(request: VersionDependentsRequest): Response<Boolean> {
        return ResponseBuilder.success(packageVersionDependentsService.delete(request))
    }

    override fun get(request: VersionDependentsRequest): Response<Set<String>> {
        return ResponseBuilder.success(packageVersionDependentsService.get(request))
    }

    override fun dependenciesReverse(
        searchStr: String,
        projectId: String?,
        repoName: String?,
        pageNumber: Int,
        pageSize: Int
    ): Response<Page<VersionDependentsRelation>> {
        return ResponseBuilder.success(
            packageVersionDependentsService.dependenciesReverse(
                searchStr,
                projectId,
                repoName,
                pageNumber,
                pageSize
            )
        )
    }
}
