package com.tencent.bkrepo.common.devops.client

import com.tencent.bkrepo.common.api.constant.AUTH_HEADER_DEVOPS_TENANT_ID
import com.tencent.bkrepo.common.api.constant.AUTH_HEADER_DEVOPS_UID
import com.tencent.bkrepo.common.api.constant.DEVOPS_PROJECT_SERVICE_NAME
import com.tencent.bkrepo.common.api.pojo.Response
import com.tencent.bkrepo.common.devops.pojo.project.ProjectCreateRequest
import com.tencent.bkrepo.common.devops.pojo.project.ProjectInfoVO
import io.swagger.annotations.Api
import io.swagger.annotations.ApiOperation
import io.swagger.annotations.ApiParam
import org.springframework.cloud.openfeign.FeignClient
import org.springframework.context.annotation.Primary
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestBody

@Api("平台权限服务项目接口")
@Primary
@FeignClient(DEVOPS_PROJECT_SERVICE_NAME, contextId = "CanwayProjectManagerClient")
@RequestMapping("/api/service")
interface CanwayProjectManagerClient {
    @ApiOperation("获取所有项目")
    @GetMapping("/projects/cw/getAllProject")
    fun getAllProject(
        @ApiParam(value = "用户ID", required = true)
        @RequestHeader(AUTH_HEADER_DEVOPS_UID)
        userId: String
    ): Response<List<ProjectInfoVO>>

    @ApiOperation("创建项目")
    @PostMapping("/open")
    fun createProject(
        @RequestHeader(AUTH_HEADER_DEVOPS_UID)
        @ApiParam(value = "用户ID", required = true)
        userId: String,
        @RequestHeader(AUTH_HEADER_DEVOPS_TENANT_ID)
        @ApiParam(value = "租户ID", required = true)
        tenantId: String,
        @RequestBody projectCreateRequest: ProjectCreateRequest
    ): Response<Boolean>
}
