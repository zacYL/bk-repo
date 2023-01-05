package com.tencent.bkrepo.maven.controller

import com.tencent.bkrepo.common.security.permission.Principal
import com.tencent.bkrepo.common.security.permission.PrincipalType
import io.swagger.annotations.Api
import io.swagger.annotations.ApiOperation
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@Api("maven debug controller")
@RequestMapping("/debug")
@Principal(PrincipalType.ADMIN)
class MavenDebugController {

    @GetMapping("/dependencies/foreach")
    @ApiOperation("补偿历史数据的依赖分析数据")
    fun dependenciesForeach() {

    }
}