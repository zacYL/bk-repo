package com.tencent.bkrepo.conan.config

import com.tencent.bkrepo.conan.interceptor.ProxyInterceptor
import com.tencent.bkrepo.conan.service.ConanRemoteService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Configuration
import org.springframework.web.servlet.config.annotation.InterceptorRegistry
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer

@Configuration
class WebMvcConfig(
    @Autowired(required = false) private val conanRemoteService: ConanRemoteService
) : WebMvcConfigurer {
    override fun addInterceptors(registry: InterceptorRegistry) {
        val interceptPaths = listOf("/**/conans/**")
        registry.addInterceptor(ProxyInterceptor(conanRemoteService))
            .addPathPatterns(interceptPaths)
    }
}
