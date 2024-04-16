package com.tencent.bkrepo.common.security.http.interceptor.anonymous

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.Ordered.LOWEST_PRECEDENCE
import org.springframework.web.servlet.config.annotation.InterceptorRegistry
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer

@Configuration
@EnableConfigurationProperties(AnonymousInterceptorProperties::class)
@ConditionalOnProperty(prefix = "security.anonymous.interceptor", name = ["enabled"], havingValue = "true")
class AnonymousInterceptorConfiguration {

    @Bean
    @ConditionalOnMissingBean
    fun anonymousAccessInterceptor(properties: AnonymousInterceptorProperties): AnonymousAccessInterceptor {
        return AnonymousAccessInterceptor(properties)
    }

    @Bean
    fun anonymousAccessInterceptorConfigure(
        properties: AnonymousInterceptorProperties,
        anonymousAccessInterceptor: AnonymousAccessInterceptor
    ): WebMvcConfigurer {
        return object : WebMvcConfigurer {
            override fun addInterceptors(registry: InterceptorRegistry) {
                val registration = registry.addInterceptor(anonymousAccessInterceptor)
                    .order(LOWEST_PRECEDENCE)
                    .excludePathPatterns("/service/**", "/replica/**")
                if (properties.excludePatterns.isNotEmpty()) {
                    registration.excludePathPatterns(properties.excludePatterns)
                }
                if (properties.includePatterns.isNotEmpty()) {
                    registration.addPathPatterns(properties.includePatterns)
                }
            }
        }
    }
}
