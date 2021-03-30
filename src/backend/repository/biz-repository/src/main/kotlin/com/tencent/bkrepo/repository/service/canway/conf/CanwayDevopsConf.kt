package com.tencent.bkrepo.repository.service.canway.conf

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.stereotype.Component

@ConfigurationProperties("devops")
@Component
data class CanwayDevopsConf(
    var host: String = "undefined"
)
