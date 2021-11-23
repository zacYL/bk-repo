package com.tencent.bkrepo.auth

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Configuration

@Configuration
@ConditionalOnProperty(prefix = "deploy", name = ["mode"], havingValue = "devops", matchIfMissing = true)
class DevopsAuthConfiguration
