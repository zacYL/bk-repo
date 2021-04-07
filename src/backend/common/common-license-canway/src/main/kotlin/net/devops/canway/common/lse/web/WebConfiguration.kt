package net.devops.canway.common.lse.web

import net.canway.license.service.LicenseAuthService
import net.devops.canway.common.lse.LseChecker
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.servlet.config.annotation.InterceptorRegistry
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer

@Configuration
@ConditionalOnProperty(prefix = "ci", value = ["license"], havingValue = "true", matchIfMissing = true)
class WebConfiguration {

    @Bean
    fun lseConfigurer(lseInterceptor: LseInterceptor): WebMvcConfigurer {
        return object : WebMvcConfigurer {
            override fun addInterceptors(registry: InterceptorRegistry) {
                registry.addInterceptor(lseInterceptor)
                    .order(0)
                super.addInterceptors(registry)
            }
        }
    }

    @Bean
    fun lseInterceptor(lseChecker: LseChecker) = LseInterceptor(lseChecker)

    @Bean
    fun lseChecker(licenseAuthService: LicenseAuthService) = LseChecker(licenseAuthService)
}
