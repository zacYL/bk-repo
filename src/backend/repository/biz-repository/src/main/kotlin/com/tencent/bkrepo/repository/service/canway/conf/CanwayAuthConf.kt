package com.tencent.bkrepo.repository.service.canway.conf

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.stereotype.Component

@ConfigurationProperties("bk")
@Component
data class CanwayAuthConf(
    var appCode: String = "bk_ci",
    var appSecret: String = "undefined",
    var host: String = "undefined"
)
