package com.tencent.bkrepo.auth.controller.user

import com.tencent.bkrepo.auth.controller.OpenResource
import com.tencent.bkrepo.auth.listener.event.admin.AdminAddEvent
import com.tencent.bkrepo.auth.listener.event.admin.AdminDeleteEvent
import com.tencent.bkrepo.auth.pojo.BatchCreateUserResponse
import com.tencent.bkrepo.auth.pojo.token.Token
import com.tencent.bkrepo.auth.pojo.token.TokenResult
import com.tencent.bkrepo.auth.pojo.user.CreateUserRequest
import com.tencent.bkrepo.auth.pojo.user.UpdateUserRequest
import com.tencent.bkrepo.auth.pojo.user.UserInfo
import com.tencent.bkrepo.auth.pojo.user.UserResult
import com.tencent.bkrepo.auth.service.PermissionService
import com.tencent.bkrepo.auth.service.UserService
import com.tencent.bkrepo.common.api.pojo.Response
import com.tencent.bkrepo.common.api.util.toJsonString
import com.tencent.bkrepo.common.security.exception.PermissionException
import com.tencent.bkrepo.common.service.util.ResponseBuilder
import com.tencent.bkrepo.common.service.util.SpringContextUtils
import io.swagger.annotations.ApiOperation
import io.swagger.annotations.ApiParam
import org.slf4j.LoggerFactory
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestAttribute
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/user")
class UserUserController(
    private val userService: UserService,
    permissionService: PermissionService
) : OpenResource(permissionService) {

    @GetMapping("/list/{projectId}")
    fun userByProjectId(
        @RequestAttribute userId: String,
        @PathVariable projectId: String,
        @RequestParam includeAdmin: Boolean = false
    ): Response<List<UserResult>> {
        preCheckUserAdmin()
        return ResponseBuilder.success(userService.listUserByProjectId(projectId, includeAdmin))
    }

    @ApiOperation("批量创建用户")
    @PostMapping("/batch")
    @Suppress("TooGenericExceptionCaught")
    fun batchCreateUsers(
        @RequestAttribute userId: String,
        @RequestBody users: List<CreateUserRequest>
    ): Response<BatchCreateUserResponse> {
        preCheckUserAdmin()
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

    @ApiOperation("更新用户信息")
    @PutMapping("/{uid}")
    fun updateById(
        @RequestAttribute userId: String,
        @ApiParam(value = "用户id")
        @PathVariable uid: String,
        @ApiParam(value = "用户更新信息")
        @RequestBody request: UpdateUserRequest
    ): Response<Boolean> {
        preCheckContextUser(userId)
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


    @ApiOperation("创建用户token")
    @PutMapping("/token")
    fun createToken(
        @RequestAttribute userId: String
    ): Response<Token?> {
        preCheckContextUser(userId)
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
        preCheckContextUser(userId)
        return ResponseBuilder.success(userService.addUserToken(userId, name, expiredAt))
    }

    @ApiOperation("查询用户token列表")
    @GetMapping("/list/token")
    fun listUserToken(
        @RequestAttribute userId: String
    ): Response<List<TokenResult>> {
        preCheckContextUser(userId)
        return ResponseBuilder.success(userService.listUserToken(userId))
    }

    @ApiOperation("删除用户token")
    @DeleteMapping("/token/{name}")
    fun deleteToken(
        @RequestAttribute userId: String,
        @ApiParam(value = "用户token")
        @PathVariable name: String
    ): Response<Boolean> {
        preCheckContextUser(userId)
        return ResponseBuilder.success(userService.removeToken(userId, name))
    }

    @ApiOperation("用户info ")
    @GetMapping("/userinfo")
    fun userInfoById(
        @RequestAttribute userId: String
    ): Response<UserInfo?> {
        preCheckContextUser(userId)
        return ResponseBuilder.success(userService.getUserInfoById(userId))
    }

    @ApiOperation("软件源--批量 添加/删除 管理员")
    @PutMapping("/admin/batch/{admin}")
    fun batchAdmin(
        @RequestAttribute userId: String,
        @ApiParam(value = "执行操作，true:代表添加为管理员；false: 删除管理员")
        @PathVariable admin: Boolean,
        @ApiParam(value = "uid 列表")
        @RequestBody list: List<String>
    ): Response<Boolean> {
        preCheckUserAdmin()
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
    }
}
