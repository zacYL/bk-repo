package net.devops.canway.common.lse.feign

import com.fasterxml.jackson.databind.ObjectMapper
import com.tencent.bkrepo.common.service.feign.ErrorCodeDecoder
import feign.Contract
import feign.RequestInterceptor
import feign.codec.Decoder
import feign.codec.Encoder
import feign.codec.ErrorDecoder
import feign.jackson.JacksonDecoder
import feign.jackson.JacksonEncoder
import feign.jaxrs.JAXRSContract
import org.springframework.context.annotation.Bean

class FeignConfiguration(
    private val objectMapper: ObjectMapper,
    private val errorCodeDecoder: ErrorCodeDecoder,
    private val requestInterceptor: RequestInterceptor
) {
    @Bean
    fun contract(): Contract {
        return JAXRSContract()
    }

    @Bean
    fun encoder(): Encoder {
        return JacksonEncoder(objectMapper)
    }

    @Bean
    fun decoder(): Decoder {
        return JacksonDecoder(objectMapper)
    }

    @Bean
    fun errorDecoder(): ErrorDecoder {
        return errorCodeDecoder
    }

    @Bean
    fun requestInterceptor(): RequestInterceptor {
        return requestInterceptor
    }
}
