package com.tencent.bkrepo.repository.cpack.controller.service

import com.tencent.bkrepo.common.api.pojo.Response
import com.tencent.bkrepo.common.artifact.pojo.RepositoryType
import com.tencent.bkrepo.common.service.util.ResponseBuilder
import com.tencent.bkrepo.repository.api.RemotePackageWhitelistClient
import com.tencent.bkrepo.common.metadata.service.whitelist.RemotePackageWhitelistService
import org.springframework.web.bind.annotation.RestController

@RestController
class RemotePackageWhitelistController(
        private val remotePackageWhitelistService: RemotePackageWhitelistService
) : RemotePackageWhitelistClient{
    override fun search(type: RepositoryType, packageKey: String?, version: String?): Response<Boolean> {
        remotePackageWhitelistService.page(
                type = type,
                packageKey = packageKey,
                version = version,
                regex = false,
                pageNumber = 1,
                pageSize = 1
        ).apply {
            return ResponseBuilder.success(this.records.isNotEmpty())
        }
    }
}
