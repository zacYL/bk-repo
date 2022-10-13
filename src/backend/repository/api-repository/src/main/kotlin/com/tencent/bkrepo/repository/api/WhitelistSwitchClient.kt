package com.tencent.bkrepo.repository.api

import com.tencent.bkrepo.common.api.constant.REPOSITORY_SERVICE_NAME
import com.tencent.bkrepo.common.api.pojo.Response
import com.tencent.bkrepo.common.artifact.pojo.RepositoryType
import org.springframework.cloud.openfeign.FeignClient
import org.springframework.context.annotation.Primary
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping

@Primary
@FeignClient(REPOSITORY_SERVICE_NAME, contextId = "WhitelistSwitchClient")
@RequestMapping("/service/remote/whitelist/switch")
interface WhitelistSwitchClient {

    @GetMapping("/{type}")
    fun get(
            @PathVariable type: RepositoryType
    ): Response<Boolean>
}