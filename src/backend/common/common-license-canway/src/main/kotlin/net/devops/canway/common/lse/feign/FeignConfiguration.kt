package net.devops.canway.common.lse.feign

import feign.Contract
import feign.jaxrs.JAXRSContract
import org.springframework.context.annotation.Bean

class FeignConfiguration {
    @Bean
    fun contract(): Contract {
        return JAXRSContract()
    }
}
