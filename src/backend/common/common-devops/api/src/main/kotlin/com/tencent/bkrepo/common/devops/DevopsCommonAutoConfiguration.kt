package com.tencent.bkrepo.common.devops

import com.tencent.bkrepo.common.devops.conf.DevopsConf
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Import

@Configuration
@ConditionalOnProperty(prefix = "auth", name = ["realm"], havingValue = DEPLOY_CANWAY, matchIfMissing = true)
@Import(
    DevopsConf::class
)
class DevopsCommonAutoConfiguration
