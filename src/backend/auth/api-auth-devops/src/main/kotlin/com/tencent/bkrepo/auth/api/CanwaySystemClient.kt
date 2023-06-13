package com.tencent.bkrepo.auth.api

import com.tencent.bkrepo.auth.constant.AuthConstant.DEVOPS_AUTH_NAME
import com.tencent.bkrepo.auth.pojo.admin.AdminVO
import io.swagger.annotations.Api
import io.swagger.annotations.ApiOperation
import io.swagger.annotations.ApiParam
import net.canway.devops.api.constants.Constants
import net.canway.devops.api.pojo.Response
import org.springframework.cloud.openfeign.FeignClient
import org.springframework.context.annotation.Primary
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping

/**
 * 平台权限服务系统接口
 */
@Api("平台权限服务系统接口")
@Primary
@FeignClient(DEVOPS_AUTH_NAME, contextId = "CanwaySystemClient")
@RequestMapping("/api/service/system")
interface CanwaySystemClient {
    @ApiOperation("获取系统管理员")
    @GetMapping("/admin/list")
    fun listAdmin(): Response<List<AdminVO>>

    @ApiOperation("判断用户是否是系统管理员")
    @GetMapping("/superior_admin")
    fun isSystemAdmin(
        @ApiParam(value = "用户ID", required = true)
        @RequestHeader(Constants.AUTH_HEADER_USER_ID)
        userId: String,
    ): Response<Boolean>
}