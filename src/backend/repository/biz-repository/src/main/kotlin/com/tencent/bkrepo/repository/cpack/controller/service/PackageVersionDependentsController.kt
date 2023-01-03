package com.tencent.bkrepo.repository.cpack.controller.service

import com.tencent.bkrepo.common.api.pojo.Page
import com.tencent.bkrepo.common.api.pojo.Response
import com.tencent.bkrepo.common.service.util.ResponseBuilder
import com.tencent.bkrepo.repository.api.PackageVersionDependentsClient
import com.tencent.bkrepo.repository.cpack.service.PackageVersionDependentsService
import com.tencent.bkrepo.repository.pojo.dependent.PackageVersionDependentsRelation
import com.tencent.bkrepo.repository.pojo.dependent.PackageVersionDependentsRequest
import org.springframework.web.bind.annotation.RestController

@RestController
class PackageVersionDependentsController(
    private val packageVersionDependentsService: PackageVersionDependentsService
) : PackageVersionDependentsClient {
    override fun insert(relation: PackageVersionDependentsRelation): Response<Boolean> {
        return ResponseBuilder.success(packageVersionDependentsService.insert(relation))
    }

    override fun delete(request: PackageVersionDependentsRequest): Response<Boolean> {
        return ResponseBuilder.success(packageVersionDependentsService.delete(request))
    }

    override fun get(request: PackageVersionDependentsRequest): Response<Set<String>> {
        return ResponseBuilder.success(packageVersionDependentsService.get(request))
    }

    override fun dependenciesReverse(
        searchStr: String,
        projectId: String?,
        repoName: String?,
        pageNumber: Int,
        pageSize: Int
    ): Response<Page<PackageVersionDependentsRelation>> {
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
