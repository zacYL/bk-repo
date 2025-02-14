package com.tencent.bkrepo.repository.cpack.controller.user

import com.tencent.bkrepo.common.api.pojo.Response
import com.tencent.bkrepo.common.artifact.pojo.RepositoryType
import com.tencent.bkrepo.common.security.permission.Principal
import com.tencent.bkrepo.common.security.permission.PrincipalType
import com.tencent.bkrepo.common.service.util.ResponseBuilder
import com.tencent.bkrepo.common.metadata.service.whitelist.WhitelistSwitchService
import com.tencent.bkrepo.common.metadata.util.WhitelistUtils
import io.swagger.annotations.ApiParam
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/remote/whitelist/switch")
@Principal(PrincipalType.ADMIN)
class UserWhitelistSwitchController(
        private var whitelistSwitchService: WhitelistSwitchService
) {
    @GetMapping("/list")
    fun list(): Response<Map<RepositoryType, Boolean>> {
        return ResponseBuilder.success(whitelistSwitchService.list())
    }

    @PostMapping("/{type}")
    fun update(
            @PathVariable type: RepositoryType,
            @ApiParam(value = "是否", required = false)
            @RequestParam status: Boolean?
    ): Response<Boolean> {
        WhitelistUtils.typeValid(type)
        return ResponseBuilder.success(whitelistSwitchService.switch(type, status))
    }
}
