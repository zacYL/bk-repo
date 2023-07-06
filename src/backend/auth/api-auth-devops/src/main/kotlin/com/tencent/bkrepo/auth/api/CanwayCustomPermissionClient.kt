package com.tencent.bkrepo.auth.api

import com.tencent.bkrepo.auth.constant.AuthConstant
import com.tencent.bkrepo.auth.pojo.permission.AnyResourcePermissionSaveDTO
import com.tencent.bkrepo.auth.pojo.permission.PermissionVO
import com.tencent.bkrepo.auth.pojo.role.*
import io.swagger.annotations.Api
import io.swagger.annotations.ApiOperation
import io.swagger.v3.oas.annotations.Parameter
import net.canway.devops.api.constants.Constants
import net.canway.devops.api.pojo.Response
import org.springframework.cloud.openfeign.FeignClient
import org.springframework.context.annotation.Primary
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.*


@Api("平台权限自定义角色接口")
@Primary
@FeignClient(AuthConstant.DEVOPS_AUTH_NAME, contextId = "CanwayCustomPermissionClient")
@RequestMapping("/api/service/custom/permission")
interface CanwayCustomPermissionClient {

    @ApiOperation("保存指定作用域下主体的任意资源权限")
    @PostMapping(
            "/{scopeCode}/{scopeId}/{subjectCode}/{subjectId}/{resourceLevel}/save",
            produces = [MediaType.APPLICATION_JSON_VALUE],
            consumes = [MediaType.APPLICATION_JSON_VALUE]
    )
    fun saveAnyPermissions(
            @RequestHeader(Constants.AUTH_HEADER_USER_ID)
            @Parameter(description = "用户ID", required = true)
            userId: String,
            @PathVariable
            @Parameter(description = "作用域ID", required = true)
            scopeId: String,
            @PathVariable
            @Parameter(description = "作用域类型", required = true)
            scopeCode: String,
            @PathVariable
            @Parameter(description = "授权主体类型", required = true)
            subjectCode: String,
            @PathVariable
            @Parameter(description = "授权主体Id", required = true)
            subjectId: String,
            @PathVariable
            @Parameter(description = "资源类型（project、tenant、system）", required = true)
            resourceLevel: String,
            @RequestBody
            permissions: List<AnyResourcePermissionSaveDTO>,
    ): Response<List<PermissionVO>>
}