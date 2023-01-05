package com.tencent.bkrepo.maven.controller

import com.tencent.bkrepo.common.api.pojo.Response
import com.tencent.bkrepo.common.security.permission.Principal
import com.tencent.bkrepo.common.security.permission.PrincipalType
import com.tencent.bkrepo.common.service.util.ResponseBuilder
import com.tencent.bkrepo.maven.service.impl.MavenDebugService
import io.swagger.annotations.Api
import io.swagger.annotations.ApiOperation
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@Api("maven debug controller")
@RequestMapping("/debug")
@Principal(PrincipalType.ADMIN)
class MavenDebugController(
    private val mavenDebugService: MavenDebugService
) {
    @GetMapping("/dependencies/foreach")
    @ApiOperation("补偿历史数据的依赖分析数据")
    fun dependenciesForeach(): Response<Boolean> {
        mavenDebugService.dependenciesForeach()
        return ResponseBuilder.success(true)
    }
}
