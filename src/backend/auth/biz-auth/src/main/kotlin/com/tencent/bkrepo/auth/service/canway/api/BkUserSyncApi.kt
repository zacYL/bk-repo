package com.tencent.bkrepo.auth.service.canway.api

import com.tencent.bkrepo.auth.constant.AUTH_API_USER_PREFIX
import io.swagger.annotations.Api
import io.swagger.annotations.ApiOperation
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping

@Api("同步蓝鲸用户")
@RequestMapping(AUTH_API_USER_PREFIX)
interface BkUserSyncApi {
    @ApiOperation("")
    @GetMapping("/sync/bk")
    fun syncBkUser()
}
