package com.tencent.bkrepo.auth.api

import com.tencent.bkrepo.auth.constant.AuthConstant.DEVOPS_AUTH_NAME
import com.tencent.bkrepo.auth.pojo.permission.ProjectPermissionAndAdminVO
import com.tencent.bkrepo.auth.pojo.permission.UserPermissionQueryDTO
import com.tencent.bkrepo.common.api.pojo.Response
import io.swagger.annotations.Api
import io.swagger.annotations.ApiOperation
import io.swagger.annotations.ApiParam
import net.canway.devops.api.constants.Constants
import com.tencent.bkrepo.auth.pojo.permission.UserPermissionValidateDTO
import com.tencent.bkrepo.auth.pojo.project.ProjectMemberVO
import io.swagger.v3.oas.annotations.Parameter
import org.springframework.cloud.openfeign.FeignClient
import org.springframework.context.annotation.Primary
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestParam
/**
 * 平台权限服务项目接口
 */
@Api("平台权限服务项目接口")
@Primary
@FeignClient(DEVOPS_AUTH_NAME, contextId = "CanwayProjectClient")
@RequestMapping("/api/service/project")
interface CanwayProjectClient {

    @ApiOperation("判断用户是否是项目或更高级别的管理员")
    @GetMapping("/{projectId}/admin/superior_admin")
    fun isProjectOrSuperiorAdmin(
        @ApiParam(value = "用户ID", required = true)
        @RequestHeader(Constants.AUTH_HEADER_USER_ID)
        userId: String,
        @ApiParam(value = "项目ID", required = true)
        @PathVariable
        projectId: String,
    ): Response<Boolean>

    @ApiOperation("检验用户是否是项目成员或管理员")
    @GetMapping("/{projectId}/member/{targetUserId}/")
    fun isProjectMemberOrAdmin(
        @ApiParam(value = "项目ID", required = true)
        @PathVariable
        projectId: String,
        @ApiParam(value = "待校验用户ID", required = true)
        @PathVariable
        targetUserId: String,
    ): Response<Boolean?>

    @ApiOperation("获取用户在资源类型下的所有权限，可以指定动作和资源实例范围")
    @PostMapping(
        "/{projectId}/permission/query",
        produces = [MediaType.APPLICATION_JSON_VALUE],
        consumes = [MediaType.APPLICATION_JSON_VALUE]
    )
    fun getUserPermission(
        @ApiParam(value = "项目ID", required = true)
        @PathVariable
        projectId: String,
        @ApiParam(value = "用户权限查询请求", required = true)
        @RequestBody
        option: UserPermissionQueryDTO,
    ): Response<ProjectPermissionAndAdminVO>

    @ApiOperation("检验用户是否拥有指定权限")
    @PostMapping(
        "/{projectId}/permission/validate",
        produces = [MediaType.APPLICATION_JSON_VALUE],
        consumes = [MediaType.APPLICATION_JSON_VALUE]
    )
    fun validateUserPermission(
        @PathVariable
        @ApiParam(value = "项目ID", required = true)
        projectId: String,
        @RequestBody
        @ApiParam(value = "用户权限校验请求", required = true)
        option: UserPermissionValidateDTO,
    ): Response<Boolean>

    @ApiOperation("获取项目成员，支持携带管理员信息")
    @GetMapping("/{projectId}/member/list")
    fun listMember(
        @PathVariable
        @Parameter(description = "项目ID", required = true)
        projectId: String,
        @RequestParam
        @Parameter(description = "是否携带禁用用户", required = false)
        withDisabled: Boolean = false,
        @RequestParam
        @Parameter(description = "是否包含租户管理员", required = false)
        withTenantAdmin: Boolean = false,
        @RequestParam
        @Parameter(description = "是否包含系统管理员", required = false)
        withSystemAdmin: Boolean = false,
        @RequestParam
        @Parameter(description = "是否包含用户关联的角色", required = false)
        withRole: Boolean = false,
    ): Response<List<ProjectMemberVO>>
}
