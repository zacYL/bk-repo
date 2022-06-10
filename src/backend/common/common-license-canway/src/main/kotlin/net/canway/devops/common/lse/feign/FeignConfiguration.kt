package net.canway.devops.common.lse.feign

import feign.jaxrs.JAXRSContract
import org.springframework.context.annotation.Bean

class FeignConfiguration {
    @Bean
    fun contract(): JAXRSContract {
        return JAXRSContract()
    }
}
