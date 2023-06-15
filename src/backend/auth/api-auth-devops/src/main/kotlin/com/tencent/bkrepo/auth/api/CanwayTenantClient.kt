package com.tencent.bkrepo.auth.api

import com.tencent.bkrepo.auth.constant.AuthConstant.DEVOPS_AUTH_NAME
import io.swagger.annotations.Api
import io.swagger.annotations.ApiOperation
import io.swagger.annotations.ApiParam
import net.canway.devops.api.constants.Constants
import net.canway.devops.api.pojo.Response
import org.springframework.cloud.openfeign.FeignClient
import org.springframework.context.annotation.Primary
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping

/**
 * 平台权限服务租户接口
 */
@Api("平台权限服务租户接口")
@Primary
@FeignClient(DEVOPS_AUTH_NAME, contextId = "CanwayTenantClient")
@RequestMapping("/api/service/tenant")
interface CanwayTenantClient {
    @ApiOperation("判断用户是否是租户成员或者租户管理员、系统管理员")
    @GetMapping("/{tenantId}/member/superior_admin")
    fun isTenantMemberOrAdmin(
        @ApiParam(value = "用户ID", required = true)
        @RequestHeader(Constants.AUTH_HEADER_USER_ID)
        userId: String,
        @ApiParam(value = "项目ID", required = true)
        @PathVariable
        tenantId: String
    ): Response<Boolean>
}