package com.tencent.bkrepo.common.artifact.whitelist

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.annotation.Configuration

@Configuration
@ConfigurationProperties("proxy-source")
data class ArtifactWhitelistProperties (
    var whitelist: String = "remote"
)