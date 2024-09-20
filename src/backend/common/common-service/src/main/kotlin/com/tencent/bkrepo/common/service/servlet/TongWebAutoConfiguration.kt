package com.tencent.bkrepo.common.service.servlet

import com.tencent.bkrepo.common.service.condition.ConditionalOnTongWeb
import com.tongweb.springboot.autoconfigure.JspPrecompilationConfigure
import com.tongweb.springboot.autoconfigure.StaticResourcesAutoConfigure
import com.tongweb.springboot.autoconfigure.StaticResourcesAutoConfigureAfter26
import com.tongweb.springboot.reactive.ReactiveTongWebWebServerFactoryAutoConfiguration
import com.tongweb.springboot.servlet.LiteTongWeb
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Import

@Configuration(proxyBeanMethods = false)
@ConditionalOnTongWeb
@Import(
    LiteTongWeb::class,
    JspPrecompilationConfigure::class,
    StaticResourcesAutoConfigure::class,
    StaticResourcesAutoConfigureAfter26::class,
    ReactiveTongWebWebServerFactoryAutoConfiguration::class
)
class TongWebAutoConfiguration
