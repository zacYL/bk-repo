package com.tencent.bkrepo.common.devops.api

import com.tencent.bkrepo.common.devops.api.conf.DevopsConf
import com.tencent.bkrepo.common.devops.api.service.BkUserService
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Import

@Configuration
@ConditionalOnProperty(prefix = "auth", name = ["realm"], havingValue = "canway", matchIfMissing = true)
@Import(
    DevopsConf::class,
    BkUserService::class
)
class DevopsCommonAutoConfiguration
