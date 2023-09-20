package com.tencent.bkrepo.common.devops.conf

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties("devops")
data class DevopsConf(
    var appCode: String = "bk_ci",
    var appSecret: String = "undefined",
    var bkHost: String = "http://localhost",
    var devopsHost: String = "http://localhost"
)
