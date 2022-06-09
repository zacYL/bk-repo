package com.tencent.bkrepo.auth.controller.user

import cn.hutool.crypto.CryptoException
import com.tencent.bkrepo.auth.constant.BKREPO_TICKET
import com.tencent.bkrepo.auth.listener.event.admin.AdminAddEvent
import com.tencent.bkrepo.auth.listener.event.admin.AdminDeleteEvent
import com.tencent.bkrepo.auth.message.AuthMessageCode
import com.tencent.bkrepo.auth.pojo.BatchCreateUserResponse
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
import com.tencent.bkrepo.common.security.util.RsaUtils
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
    private val jwtProperties: JwtAuthProperties
) {

    private val signingKey = JwtUtils.createSigningKey(jwtProperties.secretKey)

    @GetMapping("/list/{projectId}")
    fun userByProjectId(
        @RequestAttribute userId: String,
        @PathVariable projectId: String,
        @RequestParam includeAdmin: Boolean = false
    ): Response<List<UserResult>> {
        return ResponseBuilder.success(userService.listUserByProjectId(projectId, includeAdmin))
    }

    @GetMapping("/admin/{projectId}")
    fun isProjectAdmin(
        @RequestAttribute userId: String,
        @PathVariable projectId: String
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

    @GetMapping("/list")
    fun allUser(
        @RequestAttribute userId: String
    ): Response<List<UserResult>> {
        val userList = userService.listUser(listOf())
        return ResponseBuilder.success(
            userList.map {
                UserResult(
                    userId = it.userId,
                    name = it.name
                )
            }
        )
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

    @ApiOperation("批量创建用户")
    @PostMapping("/batch")
    @Principal(PrincipalType.ADMIN)
    @Suppress("TooGenericExceptionCaught")
    fun batchCreateUsers(
        @RequestAttribute userId: String,
        @RequestBody users: List<CreateUserRequest>
    ): Response<BatchCreateUserResponse> {
        var success = 0
        var failed = 0
        val failedUsers = mutableSetOf<String>()
        users.map { user ->
            try {
                if (userService.createUser(user)) {
                    success += 1
                } else {
                    failed += 1
                    failedUsers.add(user.userId)
                }
            } catch (e: Exception) {
                logger.error("Add user failed: [$user]", e)
            }
        }
        val result = BatchCreateUserResponse(success, failed, failedUsers)
        return ResponseBuilder.success(result)
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
        val operator = userService.getUserById(userId)!!
        // 将用户设置为管理员，要求操作人为管理员
        if (request.admin != null && !operator.admin) {
            throw PermissionException()
        }
        // 操作人为管理员或者操作人为自己，可以修改用户信息
        if (operator.admin || operator.userId == uid) {
            return ResponseBuilder.success(userService.updateUserById(uid, request))
        }
        throw PermissionException()
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
    @PutMapping("/token")
    fun createToken(
        @RequestAttribute userId: String
    ): Response<Token?> {
        return ResponseBuilder.success(userService.createToken(userId))
    }

    @ApiOperation("新加用户token")
    @PutMapping("/token/{name}")
    fun addUserToken(
        @RequestAttribute userId: String,
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
    @GetMapping("/list/token")
    fun listUserToken(
        @RequestAttribute userId: String
    ): Response<List<TokenResult>> {
        return ResponseBuilder.success(userService.listUserToken(userId))
    }

    @ApiOperation("删除用户token")
    @DeleteMapping("/token/{name}")
    fun deleteToken(
        @RequestAttribute userId: String,
        @ApiParam(value = "用户token")
        @PathVariable name: String
    ): Response<Boolean> {
        return ResponseBuilder.success(userService.removeToken(userId, name))
    }

    @ApiOperation("校验用户token")
    @PostMapping("/token")
    fun checkToken(
        @RequestAttribute userId: String,
        @ApiParam(value = "用户token")
        @RequestParam token: String
    ): Response<Boolean> {
        val result = userService.findUserByUserToken(userId, token) == null
        return ResponseBuilder.success(result)
    }

    @ApiOperation("获取公钥")
    @GetMapping("/rsa")
    fun getPublicKey(): Response<String?> {
        return ResponseBuilder.success(RsaUtils.publicKey)
    }

    @ApiOperation("校验用户会话token")
    @PostMapping("/login")
    fun loginUser(
        @ApiParam(value = "用户id")
        @RequestParam("uid") uid: String,
        @ApiParam(value = "用户token")
        @RequestParam("token") token: String
    ): Response<Boolean> {
        val decryptToken: String?
        try {
            decryptToken = RsaUtils.decrypt(token)
        } catch (e: CryptoException) {
            logger.error("token decrypt failed token [$uid] exception:[$e]")
            throw AuthenticationException(messageCode = AuthMessageCode.AUTH_LOGIN_FAILED)
        }
        val user = userService.findUserByUserToken(uid, decryptToken) ?: run {
            throw AuthenticationException(messageCode = AuthMessageCode.AUTH_LOGIN_FAILED)
        }
        if (user.locked) throw AuthenticationException(messageCode = AuthMessageCode.AUTH_USER_LOCKED)
        val ticket = JwtUtils.generateToken(signingKey, jwtProperties.expiration, uid)
        val cookie = Cookie(BKREPO_TICKET, ticket).apply {
            path = "/"
            maxAge = cookieExpired
        }
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
            val user = userService.getUserById(userId)
                ?: throw AuthenticationException(AuthMessageCode.AUTH_LOGIN_TOKEN_CHECK_FAILED.name)
            if (user.locked) throw AuthenticationException(AuthMessageCode.AUTH_USER_LOCKED.name)
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
        val decryptOldPwd: String?
        val decryptNewPwd: String?
        try {
            decryptOldPwd = RsaUtils.decrypt(oldPwd)
            decryptNewPwd = RsaUtils.decrypt(newPwd)
        } catch (e: CryptoException) {
            logger.warn("token decrypt failed")
            throw AuthenticationException(messageCode = AuthMessageCode.AUTH_LOGIN_TOKEN_CHECK_FAILED)
        }
        require(uid == userId) { throw PermissionException() }
        return ResponseBuilder.success(userService.updatePassword(uid, decryptOldPwd, decryptNewPwd))
    }

    @ApiOperation("用户info ")
    @GetMapping("/userinfo")
    fun userInfoById(
        @RequestAttribute userId: String
    ): Response<UserInfo?> {
        return ResponseBuilder.success(userService.getUserInfoById(userId))
    }

    @ApiOperation("重置用户密码")
    @Principal(PrincipalType.ADMIN)
    @PostMapping("/reset/{uid}")
    fun resetPassword(
        @RequestAttribute userId: String,
        @ApiParam(value = "需要重置密码用户id")
        @PathVariable uid: String,
        @ApiParam(value = "重置密码, 为空重设为默认密码", required = false)
        @RequestParam newPwd: String? = null
    ): Response<Boolean> {
        return ResponseBuilder.success(userService.resetPassword(uid, newPwd))
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

    companion object {
        private val logger = LoggerFactory.getLogger(UserUserController::class.java)
        private const val cookieExpired = 60 * 60 * 24
    }
}
