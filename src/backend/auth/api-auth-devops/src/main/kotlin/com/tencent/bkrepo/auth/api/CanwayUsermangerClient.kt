package com.tencent.bkrepo.auth.api

import com.tencent.bkrepo.auth.pojo.MigrateTokenVO
import com.tencent.bkrepo.auth.pojo.UserLoginVo
import com.tencent.bkrepo.auth.pojo.user.UserData
import com.tencent.bkrepo.auth.pojo.user.CanwayUserInfo
import com.tencent.bkrepo.auth.pojo.user.UserPasswordUpdateVO
import com.tencent.bkrepo.auth.pojo.user.UserRequest
import com.tencent.bkrepo.common.api.constant.AUTH_HEADER_DEVOPS_UID
import com.tencent.bkrepo.common.api.constant.DEVOPS_USER_MANAGER_SERVICE_NAME
import com.tencent.bkrepo.common.api.pojo.Response
import io.swagger.annotations.Api
import io.swagger.annotations.ApiOperation
import io.swagger.annotations.ApiParam
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
@FeignClient(DEVOPS_USER_MANAGER_SERVICE_NAME, contextId = "CanwayUsermangerClient")
@RequestMapping("/api/service")
interface CanwayUsermangerClient {

    @ApiOperation("第三方调用认证接口")
    @PostMapping("/authentication/login")
    fun login(
        @ApiParam(value = "用户登录信息")
        userLoginVo: UserLoginVo
    ): Response<Boolean>

    @ApiOperation("根据用户数组查询用户列表")
    @GetMapping("/list")
    fun getUserByIds(
        @ApiParam(value = "用户ID", required = true)
        @RequestParam(value = "userIds")
        userIds: List<String>
    ): Response<List<CanwayUserInfo>>

    @ApiOperation("迁移token")
    @PostMapping("/token/migrateToken")
    fun migrateToken(
        @ApiParam(value = "migrateTokenInfoList", required = true)
        migrateTokenInfoList: List<MigrateTokenVO>
    ): Response<Boolean>

    @ApiOperation("本地新增用户")
    @PostMapping("/user/add")
    fun addUsers(
        @ApiParam(value = "X-DEVOPS-UID", required = true)
        @RequestHeader(AUTH_HEADER_DEVOPS_UID)
        loginUserId: String,
        @ApiParam(value = "本地新增用户", required = true)
        userRequest: UserRequest
    ): Response<Boolean>

    @ApiOperation("获取所有用户")
    @GetMapping("/user/allUser")
    fun getAllUser(
        @ApiParam(value = "是否模糊查询", required = true)
        @RequestParam("fuzzySearch")
        fuzzySearch: Boolean,
        @ApiParam(value = "查询参数")
        @RequestParam("param")
        param: String?
    ): Response<List<UserData>>

    @ApiOperation("密码更新")
    @PostMapping("/authentication/password/update")
    fun passwordUpdate(
        @ApiParam(value = "登陆用户ID", required = true)
        @RequestHeader(AUTH_HEADER_DEVOPS_UID)
        loginUserId: String,
        @ApiParam(value = "用户账号与新密码")
        userPasswordUpdateVO: UserPasswordUpdateVO
    ): Response<String>
}
