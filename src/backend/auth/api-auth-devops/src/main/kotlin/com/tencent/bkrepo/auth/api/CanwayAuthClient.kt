package com.tencent.bkrepo.auth.api

import com.tencent.bkrepo.auth.constant.AuthConstant.AUTH_HEADER_PROJECT_ID
import com.tencent.bkrepo.auth.constant.AuthConstant.CANWAY_AUTH_SERVICE
import com.tencent.bkrepo.auth.pojo.permission.CanwayBkrepoInstance
import com.tencent.bkrepo.common.api.constant.AUTH_SERVICE_NAME
import com.tencent.bkrepo.common.api.pojo.Response
import io.swagger.annotations.Api
import io.swagger.annotations.ApiOperation
import org.springframework.cloud.openfeign.FeignClient
import org.springframework.context.annotation.Primary
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping

@Api(tags = ["Canway_Auth"], description = "平台-权限接口")
@Primary
@FeignClient(AUTH_SERVICE_NAME, contextId = "CanwayAuthClient")
@RequestMapping(CANWAY_AUTH_SERVICE)
interface CanwayAuthClient {

    @ApiOperation("平台权限实例接口")
    @GetMapping("/instanceld")
    fun instanceld(
        @RequestHeader(AUTH_HEADER_PROJECT_ID) projectId: String
    ): Response<List<CanwayBkrepoInstance>>

}