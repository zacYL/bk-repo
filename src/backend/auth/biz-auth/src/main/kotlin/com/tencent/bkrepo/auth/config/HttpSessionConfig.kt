package com.tencent.bkrepo.auth.config

import org.springframework.context.annotation.Bean
import org.springframework.session.data.mongo.JdkMongoSessionConverter
import org.springframework.session.data.mongo.config.annotation.web.http.EnableMongoHttpSession
import java.time.Duration

@EnableMongoHttpSession
class HttpSessionConfig {
    @Bean
    fun jdkMongoSessionConverter(): JdkMongoSessionConverter {
        return JdkMongoSessionConverter(Duration.ofHours(6))
    }
}