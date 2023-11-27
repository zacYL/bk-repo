package com.tencent.bkrepo.auth.api

import com.tencent.bkrepo.auth.constant.AuthConstant
import com.tencent.bkrepo.auth.pojo.role.RoleCreateDTO
import com.tencent.bkrepo.auth.pojo.role.RoleVO
import com.tencent.bkrepo.auth.pojo.role.NameAndDescDTO
import com.tencent.bkrepo.auth.pojo.role.SubjectDTO
import com.tencent.bkrepo.auth.pojo.role.RoleMemberVO
import io.swagger.annotations.Api
import io.swagger.annotations.ApiOperation
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import net.canway.devops.api.constants.Constants
import net.canway.devops.api.pojo.Response
import org.springframework.cloud.openfeign.FeignClient
import org.springframework.context.annotation.Primary
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestBody

@Api("平台权限自定义角色接口")
@Primary
@FeignClient(AuthConstant.DEVOPS_AUTH_NAME, contextId = "CanwayCustomRoleClient")
@RequestMapping("/api/service/custom/{scopeCode}/{scopeId}/role")
interface CanwayCustomRoleClient {

    @ApiOperation("创建自定义角色")
    @PostMapping(
        "/create",
        produces = [MediaType.APPLICATION_JSON_VALUE],
        consumes = [MediaType.APPLICATION_JSON_VALUE]
    )
    fun createRole(
        @RequestHeader(Constants.AUTH_HEADER_USER_ID)
        @Parameter(description = "用户ID", required = true)
        userId: String,
        @PathVariable
        @Parameter(description = "作用域ID", required = true)
        scopeId: String,
        @PathVariable
        @Parameter(description = "作用域类型", required = true)
        scopeCode: String,
        @RequestBody
        @Parameter(description = "角色")
        role: RoleCreateDTO,
    ): Response<RoleVO>

    @ApiOperation("更新自定义角色")
    @PostMapping(
        "/{id}/update",
        produces = [MediaType.APPLICATION_JSON_VALUE],
        consumes = [MediaType.APPLICATION_JSON_VALUE]
    )
    fun updateRole(
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
        @Parameter(description = "项目ID", required = true)
        id: String,
        @RequestBody
        role: NameAndDescDTO,
    ): Response<RoleVO>

    @ApiOperation("删除角色")
    @PostMapping(
        "/{id}/delete",
        produces = [MediaType.APPLICATION_JSON_VALUE],
        consumes = [MediaType.APPLICATION_JSON_VALUE]
    )
    fun deleteRole(
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
        @Parameter(description = "角色ID", required = true)
        id: String,
    ): Response<Boolean>

    @Operation(summary = "批量添加角色成员")
    @PostMapping(
        "/{id}/member/batch_add",
        produces = [MediaType.APPLICATION_JSON_VALUE],
        consumes = [MediaType.APPLICATION_JSON_VALUE]
    )
    fun batchAddRoleMember(
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
        @Parameter(description = "角色ID", required = true)
        id: String,
        @RequestBody
        @Parameter(description = "待添加成员列表", required = true)
        subjects: List<SubjectDTO>,
    ): Response<List<RoleMemberVO>>

    @Operation(summary = "批量移除角色成员")
    @PostMapping(
        "/{id}/member/batch_remove",
        produces = [MediaType.APPLICATION_JSON_VALUE],
        consumes = [MediaType.APPLICATION_JSON_VALUE]
    )
    fun batchRemoveRoleMember(
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
        @Parameter(description = "角色ID", required = true)
        id: String,
        @RequestBody
        @Parameter(description = "待移除成员列表", required = true)
        memberIds: List<String>,
    ): Response<Boolean>
}
