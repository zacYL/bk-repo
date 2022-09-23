package com.tencent.bkrepo.repository.api

import com.tencent.bkrepo.common.api.constant.REPOSITORY_SERVICE_NAME
import com.tencent.bkrepo.common.api.pojo.Response
import io.swagger.annotations.Api
import org.springframework.boot.autoconfigure.data.RepositoryType
import org.springframework.cloud.openfeign.FeignClient
import org.springframework.context.annotation.Primary
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam

@Api("远程代理仓库制品白名单接口")
@Primary
@FeignClient(REPOSITORY_SERVICE_NAME, contextId = "RemotePackageWhitelistClient")
@RequestMapping("/api/remote/whitelist")
interface UserRemoteWhitelistController {

    @GetMapping("/")
    fun search(
            @RequestParam type: RepositoryType,
            @RequestParam packageKey: String,
            @RequestParam version: String?,
    ): Response<Boolean> {
        TODO()
    }
}