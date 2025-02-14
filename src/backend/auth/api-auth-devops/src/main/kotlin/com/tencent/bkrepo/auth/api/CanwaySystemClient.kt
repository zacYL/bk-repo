package com.tencent.bkrepo.auth.api

import com.tencent.bkrepo.auth.pojo.admin.AdminVO
import com.tencent.bkrepo.common.api.constant.AUTH_HEADER_DEVOPS_UID
import com.tencent.bkrepo.common.api.constant.DEVOPS_AUTH_SERVICE_NAME
import com.tencent.bkrepo.common.api.pojo.Response
import io.swagger.annotations.Api
import io.swagger.annotations.ApiOperation
import io.swagger.annotations.ApiParam
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
@FeignClient(DEVOPS_AUTH_SERVICE_NAME, contextId = "CanwaySystemClient")
@RequestMapping("/api/service/system")
interface CanwaySystemClient {
    @ApiOperation("获取系统管理员")
    @GetMapping("/admin/list")
    fun listAdmin(): Response<List<AdminVO>>

    @ApiOperation("判断用户是否是系统管理员")
    @GetMapping("/superior_admin")
    fun isSystemAdmin(
        @ApiParam(value = "用户ID", required = true)
        @RequestHeader(AUTH_HEADER_DEVOPS_UID)
        userId: String,
    ): Response<Boolean>
}
