package net.canway.devops.common.lse.web

import net.canway.devops.common.lse.LseChecker
import net.canway.devops.common.lse.controller.LicenseController
import net.canway.devops.common.lse.service.impl.LicenseServiceImpl
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Import
import org.springframework.web.servlet.config.annotation.InterceptorRegistry
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer

@Configuration
@Import(
    LicenseController::class,
    LicenseServiceImpl::class
)
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
    fun lseChecker() = LseChecker()
}
