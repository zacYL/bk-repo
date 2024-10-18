package com.tencent.bkrepo.common.devops.client

import com.tencent.bkrepo.common.api.constant.DEVOPS_AUTH_SERVICE_NAME
import com.tencent.bkrepo.common.api.pojo.Response
import com.tencent.bkrepo.common.devops.pojo.response.DevopsTenant
import io.swagger.annotations.Api
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import org.springframework.cloud.openfeign.FeignClient
import org.springframework.context.annotation.Primary
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping

@Api("Devops 权限服务接口")
@Primary
@FeignClient(DEVOPS_AUTH_SERVICE_NAME, contextId = "ServiceTenantClient")
@RequestMapping("/api/service/tenant")
interface ServiceTenantClient {

    @Operation(summary = "查询项目ID所属的租户")
    @GetMapping("/info/project/{projectId}")
    fun getTenantByProjectId(
        @PathVariable
        @Parameter(description = "租户ID", required = true)
        projectId: String
    ): Response<DevopsTenant>
}
