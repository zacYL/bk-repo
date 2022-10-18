package com.tencent.bkrepo.repository.controller.user

import com.tencent.bkrepo.common.api.pojo.Response
import com.tencent.bkrepo.common.security.permission.Principal
import com.tencent.bkrepo.common.security.permission.PrincipalType
import com.tencent.bkrepo.common.service.util.ResponseBuilder
import com.tencent.bkrepo.repository.pojo.config.GlobalConfigInfo
import com.tencent.bkrepo.repository.service.config.GlobalConfigService
import io.swagger.annotations.Api
import io.swagger.annotations.ApiOperation
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestAttribute
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@Api("用户全局配置接口")
@RestController
@RequestMapping("/api/config")
class UserGlobalConfigController(
    private val globalConfigService: GlobalConfigService
) {
    @ApiOperation("修改配置项")
    @Principal(PrincipalType.ADMIN)
    @PostMapping("/update")
    fun updateConfig(
        @RequestAttribute userId: String,
        @RequestBody map: Map<String, Any>
    ): Response<GlobalConfigInfo?> {
        return ResponseBuilder.success(globalConfigService.updateConfig(userId, map))
    }

    @ApiOperation("查询配置信息")
    @GetMapping("/info")
    fun getConfig(): Response<GlobalConfigInfo?> {
        return ResponseBuilder.success(globalConfigService.getConfig())
    }
}
