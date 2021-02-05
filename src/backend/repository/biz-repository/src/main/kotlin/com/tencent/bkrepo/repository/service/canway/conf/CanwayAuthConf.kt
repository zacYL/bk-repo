package com.tencent.bkrepo.repository.service.canway.conf

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.stereotype.Component

@ConfigurationProperties("bk")
@Component
data class CanwayAuthConf(
    var code: String? = null,
    var secret: String? = null,
    var devops: String? = null,
    var host: String? = null
)
