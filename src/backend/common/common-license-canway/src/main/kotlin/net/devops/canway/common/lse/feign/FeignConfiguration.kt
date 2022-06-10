package net.devops.canway.common.lse.feign

import com.fasterxml.jackson.databind.ObjectMapper
import com.tencent.bkrepo.common.service.feign.ErrorCodeDecoder
import feign.Client
import feign.Contract
import feign.RequestInterceptor
import feign.codec.Decoder
import feign.codec.Encoder
import feign.codec.ErrorDecoder
import feign.jackson.JacksonDecoder
import feign.jackson.JacksonEncoder
import feign.jaxrs.JAXRSContract
import okhttp3.OkHttpClient
import org.springframework.context.annotation.Bean
import java.util.concurrent.TimeUnit

class FeignConfiguration(
//    private val objectMapper: ObjectMapper,
//    private val errorCodeDecoder: ErrorCodeDecoder,
//    private val requestInterceptor: RequestInterceptor
) {
//    @Bean
//    fun client(): Client {
//        return feign.okhttp.OkHttpClient(okHttpClient)
//    }

    @Bean
    fun contract(): JAXRSContract {
        return JAXRSContract()
    }

//    @Bean
//    fun encoder(): Encoder {
//        return JacksonEncoder()
//    }
//
//    @Bean
//    fun decoder(): Decoder {
//        return JacksonDecoder()
//    }

//    private val okHttpClient = OkHttpClient.Builder()
//        .sslSocketFactory(
//            CertTrustManager.disableValidationSSLSocketFactory,
//            CertTrustManager.disableValidationTrustManager
//        )
//        .hostnameVerifier(CertTrustManager.trustAllHostname)
//        .connectTimeout(3L, TimeUnit.SECONDS)
//        .readTimeout(5L, TimeUnit.SECONDS)
//        .writeTimeout(5L, TimeUnit.SECONDS)
//        .build()

//    @Bean
//    fun errorDecoder(): ErrorDecoder {
//        return errorCodeDecoder
//    }

//    @Bean
//    fun requestInterceptor(): RequestInterceptor {
//        return requestInterceptor
//    }
}
