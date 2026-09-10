package com.tencent.bkrepo.pypi.artifact

import org.springframework.boot.context.properties.ConfigurationProperties
import java.time.Duration

@ConfigurationProperties(prefix = "pypi")
class PypiProperties {
    var domain: String = "localhost"
    var enableRegexQuery: Boolean = true
    /**
     * 是否启用 LOCAL PyPI `/simple/{package}/` HTML 文件缓存（默认关闭）
     */
    var enableSimpleIndexCache: Boolean = false
    /**
     * 单包 simple HTML 缓存软过期时间。过期后继续返回旧缓存，并尝试刷新。
     * 小于等于 0 表示不过期。
     */
    var simpleIndexCacheTtl: Duration = Duration.ofMinutes(1)
}
