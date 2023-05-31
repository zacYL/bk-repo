package com.tencent.bkrepo.auth.controller.user

import com.tencent.bkrepo.auth.pojo.CanwayBkrepoInstance
import com.tencent.bkrepo.auth.service.CanwayAuthService
import com.tencent.bkrepo.common.service.util.ResponseBuilder
import io.swagger.annotations.Api
import io.swagger.annotations.ApiOperation
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@Api("Canway权限中心接口")
@RestController
@RequestMapping("/api/extAuth")
class ExtAuthController() {
    @Autowired
    lateinit var canwayAuthService: CanwayAuthService

    @ApiOperation("平台权限实例接口")
    @GetMapping("/instanceld")
    fun instanceld(
        @RequestHeader("X-DEVOPS-PROJECT-ID") projectId : String
    ): com.tencent.bkrepo.common.api.pojo.Response<List<CanwayBkrepoInstance>> {
        return ResponseBuilder.success(canwayAuthService.instanceld(projectId))
    }
}