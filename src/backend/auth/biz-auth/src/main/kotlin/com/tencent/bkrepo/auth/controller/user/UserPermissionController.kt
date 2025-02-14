package com.tencent.bkrepo.auth.controller.user

import com.tencent.bkrepo.auth.constant.AUTH_API_PERMISSION_PREFIX
import com.tencent.bkrepo.auth.pojo.enums.PermissionAction
import com.tencent.bkrepo.auth.pojo.enums.ResourceType
import com.tencent.bkrepo.auth.pojo.permission.CheckPermissionRequest
import com.tencent.bkrepo.auth.pojo.permission.Permission
import com.tencent.bkrepo.auth.pojo.permission.UpdatePermissionActionRequest
import com.tencent.bkrepo.auth.pojo.permission.UpdatePermissionDepartmentRequest
import com.tencent.bkrepo.auth.pojo.permission.UpdatePermissionRoleRequest
import com.tencent.bkrepo.auth.pojo.permission.UpdatePermissionUserRequest
import com.tencent.bkrepo.auth.service.PermissionService
import com.tencent.bkrepo.common.api.pojo.Response
import com.tencent.bkrepo.common.security.exception.PermissionException
import com.tencent.bkrepo.common.service.util.ResponseBuilder
import io.swagger.annotations.ApiOperation
import io.swagger.annotations.ApiParam
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestAttribute
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping(AUTH_API_PERMISSION_PREFIX)
class UserPermissionController(
    private val permissionService: PermissionService
) {

    @GetMapping("/admin")
    fun isAdmin(
        @RequestAttribute userId: String,
        @RequestParam projectId: String
    ): Response<Boolean> {
        val result = permissionService.checkPermission(
            CheckPermissionRequest(
                uid = userId,
                resourceType = ResourceType.PROJECT,
                projectId = projectId,
                action = PermissionAction.MANAGE
            )
        )
        return ResponseBuilder.success(result)
    }

    @ApiOperation("获取项目内置权限列表")
    @GetMapping("/list/inproject")
    fun listProjectBuiltinPermission(
        @ApiParam(value = "项目ID")
        @RequestParam projectId: String
    ): Response<List<Permission>> {
        return ResponseBuilder.success(permissionService.listProjectBuiltinPermission(projectId))
    }

    @ApiOperation("获取仓库内置权限列表")
    @GetMapping("/list/inrepo")
    fun listRepoBuiltinPermission(
        @ApiParam(value = "项目ID")
        @RequestParam projectId: String,
        @ApiParam(value = "仓库名称")
        @RequestParam repoName: String
    ): Response<List<Permission>> {
        return ResponseBuilder.success(permissionService.listBuiltinPermission(projectId, repoName))
    }

    @ApiOperation("更新权限绑定用户")
    @PutMapping("/user")
    fun updatePermissionUser(
        @RequestAttribute userId: String,
        @RequestBody request: UpdatePermissionUserRequest
    ): Response<Boolean> {
        val permission = permissionService.findPermissionById(request.permissionId)!!
        if (!isPermissionManager(permission.id!!, userId)) throw PermissionException()
        return ResponseBuilder.success(permissionService.updatePermissionUser(request))
    }

    @ApiOperation("更新权限绑定角色")
    @PutMapping("/role")
    fun updatePermissionRole(
        @RequestAttribute userId: String,
        @RequestBody request: UpdatePermissionRoleRequest
    ): Response<Boolean> {
        val permission = permissionService.findPermissionById(request.permissionId)!!
        if (!isPermissionManager(permission.id!!, userId)) throw PermissionException()
        return ResponseBuilder.success(permissionService.updatePermissionRole(request))
    }

    @ApiOperation("更新权限绑定部门")
    @PutMapping("/department")
    fun updatePermissionDepartment(
        @RequestAttribute userId: String,
        @RequestBody request: UpdatePermissionDepartmentRequest
    ): Response<Boolean> {
        val permission = permissionService.findPermissionById(request.permissionId)!!
        if (!isPermissionManager(permission.id!!, userId)) throw PermissionException()
        return ResponseBuilder.success(permissionService.updatePermissionDepartment(request))
    }

    @ApiOperation("更新权限绑定动作")
    @PutMapping("/action")
    fun updatePermissionAction(
        @RequestAttribute userId: String,
        @RequestBody request: UpdatePermissionActionRequest
    ): Response<Boolean> {
        val permission = permissionService.findPermissionById(request.permissionId)!!
        if (!isPermissionManager(permission.id!!, userId)) throw PermissionException()
        return ResponseBuilder.success(permissionService.updatePermissionAction(request))
    }

    private fun isPermissionManager(permissionId: String, userId: String): Boolean {
        val permission = permissionService.findPermissionById(permissionId)!!
        val repoName = when (permission.resourceType) {
            ResourceType.REPO -> permission.repos.first()
            else -> null
        }
        return permissionService.checkPermission(
            CheckPermissionRequest(
                uid = userId,
                resourceType = permission.resourceType,
                action = PermissionAction.MANAGE,
                projectId = permission.projectId!!,
                repoName = repoName
            )
        )
    }
}
