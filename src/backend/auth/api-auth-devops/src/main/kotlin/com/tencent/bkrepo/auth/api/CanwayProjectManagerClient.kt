package com.tencent.bkrepo.auth.api

import com.tencent.bkrepo.auth.constant.AuthConstant
import com.tencent.bkrepo.auth.pojo.project.ProjectCreateRequest
import com.tencent.bkrepo.auth.pojo.project.ProjectInfoVO
import com.tencent.bkrepo.common.api.pojo.Response
import io.swagger.annotations.Api
import io.swagger.annotations.ApiOperation
import io.swagger.annotations.ApiParam
import io.swagger.v3.oas.annotations.Parameter
import net.canway.devops.api.constants.Constants
import org.springframework.cloud.openfeign.FeignClient
import org.springframework.context.annotation.Primary
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestBody

@Api("平台权限服务项目接口")
@Primary
@FeignClient(AuthConstant.DEVOPS_PROJECT_NAME, contextId = "CanwayProjectManagerClient")
@RequestMapping("/api/service")
interface CanwayProjectManagerClient {
    @ApiOperation("获取所有项目")
    @GetMapping("/projects/cw/getAllProject")
    fun getAllProject(
        @ApiParam(value = "用户ID", required = true)
        @RequestHeader(Constants.AUTH_HEADER_USER_ID)
        userId: String
    ): Response<List<ProjectInfoVO>>

    @ApiOperation("创建项目")
    @PostMapping("/open")
    fun createProject(
        @RequestHeader(Constants.AUTH_HEADER_USER_ID)
        @Parameter(description = "用户ID", required = true)
        userId: String,
        @RequestHeader(Constants.AUTH_HEADER_TENANT_ID)
        @Parameter(description = "租户ID", required = true)
        tenantId: String,
        @RequestBody projectCreateRequest: ProjectCreateRequest
    ): Response<Boolean>
}
