package com.tencent.bkrepo.auth.service.canway.api

import com.tencent.bkrepo.auth.constant.AUTH_ACTION_PREFIX
import com.tencent.bkrepo.auth.constant.AUTH_SERVICE_ACTION_PREFIX
import com.tencent.bkrepo.auth.constant.AUTH_API_ACTION_PREFIX
import com.tencent.bkrepo.auth.service.canway.pojo.CanwayAction
import com.tencent.bkrepo.common.api.constant.AUTH_SERVICE_NAME
import com.tencent.bkrepo.common.api.pojo.Response
import io.swagger.annotations.Api
import io.swagger.annotations.ApiOperation
import org.springframework.cloud.openfeign.FeignClient
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping

@Api(tags = ["SERVICE_ACTION"], description = "服务-动作接口")
@FeignClient(AUTH_SERVICE_NAME, contextId = "ServiceActionResource")
@RequestMapping(AUTH_ACTION_PREFIX, AUTH_SERVICE_ACTION_PREFIX, AUTH_API_ACTION_PREFIX)
interface ServiceActionApi {
    @ApiOperation("查询仓库可选权限动作")
    @GetMapping("/{project}/{repo}/optional")
    fun listAction(
        @PathVariable project: String,
        @PathVariable repo: String
    ): Response<Set<CanwayAction>>
}
