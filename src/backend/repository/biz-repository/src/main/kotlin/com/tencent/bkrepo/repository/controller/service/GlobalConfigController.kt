package com.tencent.bkrepo.repository.controller.service

import com.tencent.bkrepo.common.api.pojo.Response
import com.tencent.bkrepo.common.service.util.ResponseBuilder
import com.tencent.bkrepo.repository.api.GlobalConfigClient
import com.tencent.bkrepo.repository.pojo.config.ConfigType
import com.tencent.bkrepo.repository.pojo.config.GlobalConfigInfo
import com.tencent.bkrepo.repository.service.config.GlobalConfigService
import org.springframework.web.bind.annotation.RestController

@RestController
class GlobalConfigController(
    private val globalConfigService: GlobalConfigService
) : GlobalConfigClient {
    override fun getConfig(type: ConfigType): Response<GlobalConfigInfo?> {
        return ResponseBuilder.success(globalConfigService.getConfig(type))
    }
}
