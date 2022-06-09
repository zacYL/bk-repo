package net.devops.canway.common.lse.web

import net.devops.canway.common.lse.LseChecker
import net.devops.canway.common.lse.feign.LicenseFeign
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.cloud.openfeign.EnableFeignClients
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.servlet.config.annotation.InterceptorRegistry
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer

@Configuration
@EnableFeignClients(basePackages = ["net.devops.canway"])
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
    fun lseChecker(licenseFeign: LicenseFeign) = LseChecker(licenseFeign)
}
