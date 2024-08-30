package com.tencent.bkrepo.common.service.servlet

import com.tongweb.springboot.autoconfigure.JspPrecompilationConfigure
import com.tongweb.springboot.autoconfigure.StaticResourcesAutoConfigure
import com.tongweb.springboot.autoconfigure.StaticResourcesAutoConfigureAfter26
import com.tongweb.springboot.reactive.ReactiveTongWebWebServerFactoryAutoConfiguration
import com.tongweb.springboot.servlet.LiteTongWeb
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Import

@Configuration(proxyBeanMethods = false)
@ConditionalOnProperty(name = ["devops.server.type"], havingValue = "tongweb", matchIfMissing = false)
@Import(
    LiteTongWeb::class,
    JspPrecompilationConfigure::class,
    StaticResourcesAutoConfigure::class,
    StaticResourcesAutoConfigureAfter26::class,
    ReactiveTongWebWebServerFactoryAutoConfiguration::class
)
class TongWebAutoConfiguration
