package com.tencent.bkrepo.auth.controller.user

import com.tencent.bkrepo.auth.pojo.role.Role
import com.tencent.bkrepo.auth.service.PermissionService
import com.tencent.bkrepo.auth.service.RoleService
import com.tencent.bkrepo.common.api.pojo.Response
import com.tencent.bkrepo.common.security.exception.PermissionException
import com.tencent.bkrepo.common.service.util.ResponseBuilder
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestAttribute
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
}
