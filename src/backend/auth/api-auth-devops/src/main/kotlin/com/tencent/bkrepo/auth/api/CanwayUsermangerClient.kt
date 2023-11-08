package com.tencent.bkrepo.auth.api

import com.tencent.bkrepo.auth.constant.AuthConstant
import com.tencent.bkrepo.auth.pojo.MigrateTokenVO
import com.tencent.bkrepo.auth.pojo.UserLoginVo
import com.tencent.bkrepo.auth.pojo.user.UserData
import com.tencent.bkrepo.auth.pojo.user.UserInfo
import com.tencent.bkrepo.auth.pojo.user.UserPasswordUpdateVO
import com.tencent.bkrepo.auth.pojo.user.UserRequest
import io.swagger.annotations.Api
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import net.canway.devops.api.constants.Constants
import net.canway.devops.api.pojo.Response
import org.springframework.cloud.openfeign.FeignClient
import org.springframework.context.annotation.Primary
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RequestHeader

/**
 * 平台用户服务项目接口
 */
@Api("平台用户服务项目接口")
@Primary
@FeignClient(AuthConstant.DEVOPS_USER_NAME, contextId = "CanwayUsermangerClient")
@RequestMapping("/api/service")
interface CanwayUsermangerClient {

    @Operation(summary = "第三方调用认证接口")
    @PostMapping("/authentication/login")
    fun login(
        @Parameter(name = "用户登录信息")
        userLoginVo: UserLoginVo
    ): Response<Boolean>

    @Operation(summary = "根据用户数组查询用户列表")
    @GetMapping("/list")
    fun getUserByIds(
        @Parameter(name = "用户ID", required = true)
        @RequestParam(value = "userIds")
        userIds: List<String>
    ): Response<List<UserInfo>>

    @Operation(summary = "迁移token")
    @PostMapping("/token/migrateToken")
    fun migrateToken(
        @Parameter(name = "migrateTokenInfoList", required = true)
        migrateTokenInfoList: List<MigrateTokenVO>
    ): Response<Boolean>

    @Operation(summary = "本地新增用户")
    @PostMapping("/user/add")
    fun addUsers(
        @Parameter(name = "X-DEVOPS-UID", required = true)
        @RequestHeader(Constants.AUTH_HEADER_USER_ID)
        loginUserId: String,
        @Parameter(name = "本地新增用户", required = true)
        userRequest: UserRequest
    ): Response<Boolean>

    @Operation(summary = "获取所有用户")
    @GetMapping("/user/allUser")
    fun getAllUser(
        @Parameter(name = "是否模糊查询", required = true)
        @RequestParam("fuzzySearch")
        fuzzySearch: Boolean,
        @Parameter(name = "查询参数")
        @RequestParam("param")
        param: String?
    ): Response<List<UserData>>

    @Operation(summary = "密码更新")
    @PostMapping("/authentication/password/update")
    fun passwordUpdate(
        @Parameter(name = "登陆用户ID", required = true)
        @RequestHeader(Constants.AUTH_HEADER_USER_ID)
        loginUserId: String,
        @Parameter(name = "用户账号与新密码")
        userPasswordUpdateVO: UserPasswordUpdateVO
    ): Response<String>
}
