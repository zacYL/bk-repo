package com.tencent.bkrepo.auth.controller.user

import com.tencent.bkrepo.auth.constant.BKREPO_TICKET
import com.tencent.bkrepo.auth.constant.PROJECT_MANAGE_PERMISSION
import com.tencent.bkrepo.auth.constant.PROJECT_VIEW_PERMISSION
import com.tencent.bkrepo.auth.listener.event.admin.AdminAddEvent
import com.tencent.bkrepo.auth.listener.event.admin.AdminDeleteEvent
import com.tencent.bkrepo.auth.message.AuthMessageCode
import com.tencent.bkrepo.auth.pojo.enums.PermissionAction
import com.tencent.bkrepo.auth.pojo.enums.ResourceType
import com.tencent.bkrepo.auth.pojo.permission.CheckPermissionRequest
import com.tencent.bkrepo.auth.pojo.token.Token
import com.tencent.bkrepo.auth.pojo.token.TokenResult
import com.tencent.bkrepo.auth.pojo.user.CreateUserRequest
import com.tencent.bkrepo.auth.pojo.user.UpdateUserRequest
import com.tencent.bkrepo.auth.pojo.user.User
import com.tencent.bkrepo.auth.pojo.user.UserInfo
import com.tencent.bkrepo.auth.pojo.user.UserResult
import com.tencent.bkrepo.auth.service.PermissionService
import com.tencent.bkrepo.auth.service.RoleService
import com.tencent.bkrepo.auth.service.UserService
import com.tencent.bkrepo.common.api.exception.ErrorCodeException
import com.tencent.bkrepo.common.api.pojo.Page
import com.tencent.bkrepo.common.api.pojo.Response
import com.tencent.bkrepo.common.api.util.toJsonString
import com.tencent.bkrepo.common.security.exception.AuthenticationException
import com.tencent.bkrepo.common.security.exception.PermissionException
import com.tencent.bkrepo.common.security.http.jwt.JwtAuthProperties
import com.tencent.bkrepo.common.security.permission.Principal
import com.tencent.bkrepo.common.security.permission.PrincipalType
import com.tencent.bkrepo.common.security.util.JwtUtils
import com.tencent.bkrepo.common.service.util.HttpContextHolder
import com.tencent.bkrepo.common.service.util.ResponseBuilder
import com.tencent.bkrepo.common.service.util.SpringContextUtils
import io.swagger.annotations.ApiOperation
import io.swagger.annotations.ApiParam
import org.slf4j.LoggerFactory
import org.springframework.web.bind.annotation.CookieValue
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestAttribute
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import javax.servlet.http.Cookie

@RestController
@RequestMapping("/api/user")
class UserUserController(
    private val permissionService: PermissionService,
    private val userService: UserService,
    private val roleService: RoleService,
    private val jwtProperties: JwtAuthProperties
) {

    private val signingKey = JwtUtils.createSigningKey(jwtProperties.secretKey)

    @GetMapping("/list/{projectId}")
    fun userByProjectId(
        @RequestAttribute userId: String,
        @PathVariable projectId: String,
        @RequestParam includeAdmin: Boolean = false
    ): Response<List<UserResult>> {
        if (permissionService.isProjectManager(userId)) return ResponseBuilder.success(
            userService.listUser(listOf()).map {
                UserResult(
                    userId = it.userId,
                    name = it.name
                )
            })
        val permissions = permissionService.listProjectBuiltinPermission(projectId)
        val users = mutableSetOf<String>()
        val roles = mutableSetOf<String>()
        for (permission in permissions) {
            logger.info("$permission")
            if (projectBuiltinPermission.contains(permission.permName)) users.addAll(permission.users)
            if (permission.permName == PROJECT_VIEW_PERMISSION) roles.addAll(permission.roles)
        }
        for (role in roles) {
            users.addAll(roleService.listUserByRoleId(role).map{ it.userId })
        }
        if(includeAdmin) users.addAll(userService.listAdminUser().map { it.userId })
        val userList = userService.listUser(listOf()).filter { users.contains(it.userId) }
        val result =  userList.map { UserResult(userId = it.userId, name = it.name) }
        return ResponseBuilder.success(result)
    }

    @GetMapping("/admin/{projectId}")
    fun isProjectAdmin(
        @RequestAttribute userId: String,
        @PathVariable projectId: String): Response<Boolean> {
        val result = permissionService.checkPermission(
            CheckPermissionRequest(
                uid = userId,
                resourceType = ResourceType.PROJECT.name,
                projectId = projectId,
                action = PermissionAction.MANAGE.name
            )
        )
        return ResponseBuilder.success(result)
    }

    @GetMapping("/list")
    fun allUser(
        @RequestAttribute userId: String
    ): Response<List<UserResult>> {
        val userList = userService.listUser(listOf())
        return ResponseBuilder.success(userList.map {
            UserResult(
                userId = it.userId,
                name = it.name
            )
        })
    }

    @ApiOperation("创建用户")
    @PostMapping("/create")
    @Principal(PrincipalType.ADMIN)
    fun createUser(
        @RequestAttribute userId: String,
        @RequestBody request: CreateUserRequest
    ): Response<Boolean> {
        return ResponseBuilder.success(userService.createUser(request))
    }

    @ApiOperation("删除用户")
    @DeleteMapping("/{uid}")
    @Principal(PrincipalType.ADMIN)
    fun deleteById(
        @RequestAttribute userId: String,
        @ApiParam(value = "用户id")
        @PathVariable uid: String
    ): Response<Boolean> {
        return ResponseBuilder.success(userService.deleteById(uid))
    }

    @ApiOperation("用户详情")
    @GetMapping("/detail/{uid}")
    fun detail(
        @RequestAttribute userId: String,
        @ApiParam(value = "用户id")
        @PathVariable uid: String
    ): Response<User?> {
        return ResponseBuilder.success(userService.getUserById(uid))
    }

    @ApiOperation("更新用户信息")
    @PutMapping("/{uid}")
    fun updateById(
        @RequestAttribute userId: String,
        @ApiParam(value = "用户id")
        @PathVariable uid: String,
        @ApiParam(value = "用户更新信息")
        @RequestBody request: UpdateUserRequest
    ): Response<Boolean> {
        if (request.admin != null) {
            val operator = userService.getUserById(userId)!!
            if(!operator.admin) throw PermissionException()
        }
        return ResponseBuilder.success(userService.updateUserById(uid, request))
    }

    @ApiOperation("新增用户所属角色")
    @PostMapping("/role/{uid}/{rid}")
    fun addUserRole(
        @RequestAttribute userId: String,
        @ApiParam(value = "用户id")
        @PathVariable uid: String,
        @ApiParam(value = "用户角色id")
        @PathVariable rid: String
    ): Response<User?> {
        return ResponseBuilder.success(userService.addUserToRole(uid, rid))
    }

    @ApiOperation("删除用户所属角色")
    @DeleteMapping("/role/{uid}/{rid}")
    fun removeUserRole(
        @RequestAttribute userId: String,
        @ApiParam(value = "用户id")
        @PathVariable uid: String,
        @ApiParam(value = "用户角色")
        @PathVariable rid: String
    ): Response<User?> {
        return ResponseBuilder.success(userService.removeUserFromRole(uid, rid))
    }

    @ApiOperation("批量新增用户所属角色")
    @PatchMapping("/role/add/{rid}")
    fun addUserRoleBatch(
        @RequestAttribute userId: String,
        @ApiParam(value = "用户角色Id")
        @PathVariable rid: String,
        @ApiParam(value = "用户id集合")
        @RequestBody request: List<String>
    ): Response<Boolean> {
        return ResponseBuilder.success(userService.addUserToRoleBatch(request, rid))
    }

    @ApiOperation("批量删除用户所属角色")
    @PatchMapping("/role/delete/{rid}")
    fun deleteUserRoleBatch(
        @RequestAttribute userId: String,
        @ApiParam(value = "用户角色Id")
        @PathVariable rid: String,
        @ApiParam(value = "用户id集合")
        @RequestBody request: List<String>
    ): Response<Boolean> {
        return ResponseBuilder.success(userService.removeUserFromRoleBatch(request, rid))
    }

    @ApiOperation("创建用户token")
    @PostMapping("/token/{uid}")
    fun createToken(
        @RequestAttribute userId: String
    ): Response<Token?> {
        return ResponseBuilder.success(userService.createToken(userId))
    }

    @ApiOperation("新加用户token")
    @PostMapping("/token/{uid}/{name}")
    fun addUserToken(
        @RequestAttribute userId: String,
        @ApiParam(value = "用户id")
        @PathVariable("uid") uid: String,
        @ApiParam(value = "name")
        @PathVariable("name") name: String,
        @ApiParam(value = "expiredAt", required = false)
        @RequestParam expiredAt: String?,
        @ApiParam(value = "projectId", required = false)
        @RequestParam projectId: String?
    ): Response<Token?> {
        return ResponseBuilder.success(userService.addUserToken(userId, name, expiredAt))
    }

    @ApiOperation("查询用户token列表")
    @GetMapping("/list/token/{uid}")
    fun listUserToken(
        @RequestAttribute userId: String,
        @ApiParam(value = "用户id")
        @PathVariable("uid") uid: String
    ): Response<List<TokenResult>> {
        return ResponseBuilder.success(userService.listUserToken(userId))
    }

    @ApiOperation("删除用户token")
    @DeleteMapping("/token/{uid}/{name}")
    fun deleteToken(
        @RequestAttribute userId: String,
        @ApiParam(value = "用户id")
        @PathVariable uid: String,
        @ApiParam(value = "用户token")
        @PathVariable name: String
    ): Response<Boolean> {
        return ResponseBuilder.success(userService.removeToken(userId, name))
    }

    @ApiOperation("校验用户token")
    @GetMapping("/token/{uid}/{token}")
    @Deprecated("接口改为post方式")
    fun checkUserToken(
        @RequestAttribute userId: String,
        @ApiParam(value = "用户id")
        @PathVariable uid: String,
        @ApiParam(value = "用户token")
        @PathVariable token: String
    ): Response<Boolean> {
        val result = if(userService.findUserByUserToken(userId, token) == null) false else false
        return ResponseBuilder.success(result)
    }

    @ApiOperation("校验用户token")
    @PostMapping("/token")
    fun checkToken(
        @RequestAttribute userId: String,
        @ApiParam(value = "用户id")
        @RequestParam uid: String,
        @ApiParam(value = "用户token")
        @RequestParam token: String
    ): Response<Boolean> {
        val result = if(userService.findUserByUserToken(userId, token) == null) false else false
        return ResponseBuilder.success(result)
    }

    @ApiOperation("校验用户会话token")
    @PostMapping("/login")
    fun loginUser(
        @ApiParam(value = "用户id")
        @RequestParam("uid") uid: String,
        @ApiParam(value = "用户token")
        @RequestParam("token") token: String
    ): Response<Boolean> {
        userService.findUserByUserToken(uid, token) ?: run {
            return ResponseBuilder.success(false)
        }
        val ticket = JwtUtils.generateToken(signingKey, jwtProperties.expiration, uid)
        val cookie = Cookie(BKREPO_TICKET, ticket)
        cookie.path = "/"
        cookie.maxAge = 60 * 60 * 24
        HttpContextHolder.getResponse().addCookie(cookie)
        return ResponseBuilder.success(true)
    }

    @ApiOperation("获取用户信息")
    @GetMapping("/info")
    fun userInfo(
        @ApiParam(value = "用户id")
        @CookieValue(value = "bkrepo_ticket") bkrepoToken: String?
    ): Response<Map<String, Any>> {
        try {
            bkrepoToken ?: run {
                throw IllegalArgumentException("ticket can not be null")
            }
            val userId = JwtUtils.validateToken(signingKey, bkrepoToken).body.subject
            val result = mapOf("userId" to userId)
            return ResponseBuilder.success(result)
        } catch (ignored: Exception) {
            logger.warn("validate user token false [$bkrepoToken]")
            throw AuthenticationException(AuthMessageCode.AUTH_LOGIN_TOKEN_CHECK_FAILED.name)
        }
    }

    @ApiOperation("校验用户ticket")
    @GetMapping("/verify")
    fun verify(
        @ApiParam(value = "用户id")
        @RequestParam(value = "bkrepo_ticket") bkrepoToken: String?
    ): Response<Map<String, Any>> {
        try {
            bkrepoToken ?: run {
                throw IllegalArgumentException("ticket can not be null")
            }
            val userId = JwtUtils.validateToken(signingKey, bkrepoToken).body.subject
            val result = mapOf("user_id" to userId)
            return ResponseBuilder.success(result)
        } catch (ignored: Exception) {
            logger.warn("validate user token false [$bkrepoToken]")
            throw ErrorCodeException(AuthMessageCode.AUTH_LOGIN_TOKEN_CHECK_FAILED)
        }
    }

    @ApiOperation("用户分页列表")
    @GetMapping("page/{pageNumber}/{pageSize}")
    @Principal(PrincipalType.ADMIN)
    fun userPage(
        @RequestAttribute userId: String,
        @PathVariable pageNumber: Int,
        @PathVariable pageSize: Int,
        @RequestParam user: String? = null,
        @RequestParam admin: Boolean?,
        @RequestParam locked: Boolean?
    ): Response<Page<UserInfo>> {
        val result = userService.userPage(pageNumber, pageSize, user, admin, locked)
        return ResponseBuilder.success(result)
    }

    @ApiOperation("修改用户密码")
    @PutMapping("/update/password/{uid}")
    fun updatePassword(
        @RequestAttribute userId: String,
        @PathVariable uid: String,
        @RequestParam oldPwd: String,
        @RequestParam newPwd: String
    ): Response<Boolean> {
        return ResponseBuilder.success(userService.updatePassword(uid, oldPwd, newPwd))
    }

    @ApiOperation("用户info ")
    @GetMapping("/userinfo/{uid}")
    fun userInfoById(
        @RequestAttribute userId: String,
        @PathVariable uid: String
    ): Response<UserInfo?> {
        return ResponseBuilder.success(userService.getUserInfoById(uid))
    }

    @ApiOperation("重置用户密码 ")
    @GetMapping("/reset/{uid}")
    fun resetPassword(
        @RequestAttribute userId: String,
        @PathVariable uid: String
    ): Response<Boolean> {
        return ResponseBuilder.success(userService.resetPassword(uid))
    }

    @ApiOperation("检验系统中是否存在同名userId ")
    @Principal(PrincipalType.ADMIN)
    @GetMapping("/repeat/{uid}")
    fun repeatUid(
        @RequestAttribute userId: String,
        @PathVariable uid: String
    ): Response<Boolean> {
        return ResponseBuilder.success(userService.repeatUid(uid))
    }

    @ApiOperation("软件源--批量 添加/删除 管理员")
    @Principal(PrincipalType.ADMIN)
    @PutMapping("/admin/batch/{admin}")
    fun batchAdmin(
        @RequestAttribute userId: String,
        @ApiParam(value = "执行操作，true:代表添加为管理员；false: 删除管理员")
        @PathVariable admin: Boolean,
        @ApiParam(value = "uid 列表")
        @RequestBody list: List<String>
    ): Response<Boolean> {
        val successId = mutableSetOf<String>()
        for (uid in list) {
            userService.updateUserById(uid, UpdateUserRequest(admin = admin))
            successId.add(uid)
        }
        if (admin) {
            SpringContextUtils.publishEvent(
                AdminAddEvent(
                    resourceKey = successId.toList().toJsonString(),
                    userId = userId
                )
            )
        } else {
            SpringContextUtils.publishEvent(
                AdminDeleteEvent(
                    resourceKey = successId.toList().toJsonString(),
                    userId = userId
                )
            )
        }
        return ResponseBuilder.success()
    }

    companion object{
        private val logger = LoggerFactory.getLogger(UserUserController::class.java)
        val projectBuiltinPermission = listOf(PROJECT_MANAGE_PERMISSION, PROJECT_VIEW_PERMISSION)
    }
}
