package com.tencent.bkrepo.common.cpack.conf

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties("cpack")
data class CpackConf(
    var host: String = "undefined"
)
