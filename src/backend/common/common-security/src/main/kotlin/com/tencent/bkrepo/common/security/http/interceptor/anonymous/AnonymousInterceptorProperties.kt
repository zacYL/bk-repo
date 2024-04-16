package com.tencent.bkrepo.common.security.http.interceptor.anonymous

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.NestedConfigurationProperty

@ConfigurationProperties("security.anonymous.interceptor")
data class AnonymousInterceptorProperties(
    var enabled: Boolean = false,
    @NestedConfigurationProperty
    var cidr: CIDRProperties = CIDRProperties(),
    var excludePatterns: List<String> = emptyList(),
    var includePatterns: List<String> = emptyList()
) {
    data class CIDRProperties(
        var blackList: List<String> = emptyList(),
        var whiteList: List<String> = emptyList()
    )
}
