package com.tencent.bkrepo.opdata.config

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.stereotype.Component

@ConfigurationProperties("op")
@Component
data class OpDataJobConfig(
    var frequency: Int = 10
)