package com.tencent.bkrepo.repository.cpack.controller.service

import com.tencent.bkrepo.common.api.pojo.Response
import com.tencent.bkrepo.common.artifact.pojo.RepositoryType
import com.tencent.bkrepo.common.service.util.ResponseBuilder
import com.tencent.bkrepo.repository.api.WhitelistSwitchClient
import com.tencent.bkrepo.common.metadata.service.whitelist.WhitelistSwitchService
import org.springframework.web.bind.annotation.RestController

@RestController
class WhitelistSwitchController(
        private val whitelistSwitchService: WhitelistSwitchService
) : WhitelistSwitchClient {
    override fun get(type: RepositoryType): Response<Boolean> {
        return ResponseBuilder.success(whitelistSwitchService.get(type))
    }
}
