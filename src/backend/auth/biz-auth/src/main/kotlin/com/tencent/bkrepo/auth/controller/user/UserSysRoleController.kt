package com.tencent.bkrepo.auth.controller.user

import com.tencent.bkrepo.auth.pojo.role.CreateRoleRequest
import com.tencent.bkrepo.auth.pojo.role.Role
import com.tencent.bkrepo.auth.service.PermissionService
import com.tencent.bkrepo.auth.service.RoleService
import com.tencent.bkrepo.common.api.pojo.Response
import com.tencent.bkrepo.common.security.exception.PermissionException
import com.tencent.bkrepo.common.security.permission.Principal
import com.tencent.bkrepo.common.security.permission.PrincipalType
import com.tencent.bkrepo.common.service.util.ResponseBuilder
import io.swagger.annotations.ApiOperation
import io.swagger.annotations.ApiParam
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestAttribute
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/sys/role")
class UserSysRoleController(
    private val permissionService: PermissionService,
    private val roleService: RoleService
) {

    @GetMapping("/list/{projectId}")
    fun roleByProjectId(
        @RequestAttribute userId: String,
        @PathVariable projectId: String
    ): Response<List<Role>> {
        return ResponseBuilder.success(roleService.systemRolesByProjectId(projectId))
    }

    @GetMapping("/list")
    fun allRole(
        @RequestAttribute userId: String
    ): Response<List<Role>> {
        if (!permissionService.isProjectManager(userId)) throw PermissionException()
        return ResponseBuilder.success(roleService.systemRoles())
    }

    @ApiOperation("删除角色")
    @DeleteMapping("/delete/{id}")
    @Principal(PrincipalType.ADMIN)
    fun deleteRole(
        @RequestAttribute userId: String,
        @ApiParam(value = "角色主键id")
        @PathVariable id: String
    ): Response<Boolean> {
        roleService.deleteRoleByid(id)
        return ResponseBuilder.success()
    }

    @ApiOperation("创建角色")
    @PostMapping("/create")
    @Principal(PrincipalType.ADMIN)
    fun createRole(
        @RequestAttribute userId: String,
        @RequestBody request: CreateRoleRequest
    ): Response<String?> {
        val id = roleService.createRole(request)
        return ResponseBuilder.success(id)
    }
}
