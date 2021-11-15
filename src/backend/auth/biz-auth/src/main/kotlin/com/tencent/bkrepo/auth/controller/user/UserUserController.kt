package com.tencent.bkrepo.auth.controller.user

import com.tencent.bkrepo.auth.constant.PROJECT_MANAGE_PERMISSION
import com.tencent.bkrepo.auth.constant.PROJECT_VIEW_PERMISSION
import com.tencent.bkrepo.auth.pojo.user.UserResult
import com.tencent.bkrepo.auth.service.PermissionService
import com.tencent.bkrepo.auth.service.RoleService
import com.tencent.bkrepo.auth.service.UserService
import com.tencent.bkrepo.common.api.pojo.Response
import com.tencent.bkrepo.common.service.util.ResponseBuilder
import org.slf4j.LoggerFactory
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/user")
class UserUserController(
    private val permissionService: PermissionService,
    private val userService: UserService,
    private val roleService: RoleService
) {

    @GetMapping("/list/{projectId}")
    fun userByProjectId(
//        @RequestAttribute userId: String,
        @PathVariable projectId: String,
        @RequestParam includeAdmin: Boolean = false
    ): Response<List<UserResult>> {
        val permissions = permissionService.listProjectBuiltinPermission(projectId)
        val users = mutableSetOf<String>()
        val roles = mutableSetOf<String>()
        for (permission in permissions) {
            logger.info("$permission")
            if (projectBuiltinPermission.contains(permission.permName)) users.addAll(permission.users)
            if (permission.permName == PROJECT_VIEW_PERMISSION) roles.addAll(permission.roles)
        }
        for (role in roles) {
            logger.info("$role")
            users.addAll(roleService.listUserByRoleId(role).map{ it.userId })
        }
        if(includeAdmin) users.addAll(userService.listAdminUser().map { it.userId })
        val userList = userService.listUser(listOf()).filter { users.contains(it.userId) }
        val result =  userList.map { UserResult(userId = it.userId, name = it.name) }
        return ResponseBuilder.success(result)
    }

//    @GetMapping("/admin/{projectId}")
//    fun isProjectAdmin(@PathVariable projectId: String): Response<Boolean> {
//        permissionService.listProjectBuiltinPermission()
//    }

    @GetMapping("/list")
    fun allUser(
        @RequestAttribute userId: String
    ): Response<List<UserResult>> {
        val user = userService.getUserById(userId)!!
//        if(!user.admin) throw PermissionException()
        val userList = userService.listUser(listOf())
        return ResponseBuilder.success(userList.map {
            UserResult(
                userId = it.userId,
                name = it.name
            )
        })
    }

    companion object{
        private val logger = LoggerFactory.getLogger(UserUserController::class.java)
        val projectBuiltinPermission = listOf(PROJECT_MANAGE_PERMISSION, PROJECT_VIEW_PERMISSION)
    }
}
