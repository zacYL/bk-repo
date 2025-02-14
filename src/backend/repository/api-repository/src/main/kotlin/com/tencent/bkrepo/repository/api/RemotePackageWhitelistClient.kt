package com.tencent.bkrepo.repository.api

import com.tencent.bkrepo.common.api.constant.REPOSITORY_SERVICE_NAME
import com.tencent.bkrepo.common.api.pojo.Response
import com.tencent.bkrepo.common.artifact.pojo.RepositoryType
import org.springframework.cloud.openfeign.FeignClient
import org.springframework.context.annotation.Primary
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam

@Primary
@FeignClient(REPOSITORY_SERVICE_NAME, contextId = "RemotePackageWhitelistClient")
@RequestMapping("/service/remote/whitelist")
interface RemotePackageWhitelistClient {
    @GetMapping("/")
    fun search(
            @RequestParam type: RepositoryType,
            @RequestParam packageKey: String? = null,
            @RequestParam version: String? = null,
    ): Response<Boolean>
}