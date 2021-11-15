package com.tencent.bkrepo.auth.controller.user

import com.tencent.bkrepo.auth.constant.PROJECT_MANAGE_ID
import com.tencent.bkrepo.auth.constant.PROJECT_VIEW_ID
import com.tencent.bkrepo.auth.constant.PROJECT_VIEW_PERMISSION
import com.tencent.bkrepo.auth.pojo.role.Role
import com.tencent.bkrepo.auth.service.PermissionService
import com.tencent.bkrepo.auth.service.RoleService
import com.tencent.bkrepo.auth.service.UserService
import com.tencent.bkrepo.common.api.pojo.Response
import com.tencent.bkrepo.common.security.exception.PermissionException
import com.tencent.bkrepo.common.service.util.ResponseBuilder
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestAttribute
import org.springframework.web.bind.annotation.PathVariable

@RestController
@RequestMapping("/api/sys/role")
class UserRoleController(
    private val permissionService: PermissionService,
    private val userService: UserService,
    private val roleService: RoleService
) {

    @GetMapping("/list/{projectId}")
    fun userByProjectId(
//        @RequestAttribute userId: String,
        @PathVariable projectId: String
    ): Response<List<Role>> {
        val permissions = permissionService.listPermission(projectId, null)
        val roles = mutableSetOf<String>()
        for (permission in permissions) {
            if (permission.permName == PROJECT_VIEW_PERMISSION) roles.addAll(permission.roles)
        }
        val roleList = roleService.systemRoles().filter { roles.contains(it.id) }
        return ResponseBuilder.success(roleList)
    }

    @GetMapping("/list")
    fun allUser(
//        @RequestAttribute userId: String
    ): Response<List<Role>> {
//        val user = userService.getUserById(userId)!!
//        if(!user.admin) throw PermissionException()
        return ResponseBuilder.success(roleService.systemRoles())
    }

    companion object{
        val projectRole = listOf(PROJECT_MANAGE_ID, PROJECT_VIEW_ID)
    }
}
