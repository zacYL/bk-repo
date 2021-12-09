package com.tencent.bkrepo.auth

import com.tencent.bkrepo.auth.service.impl.CanwayDepartmentServiceImpl
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Import

@Configuration
@ConditionalOnProperty(prefix = "auth", name = ["realm"], havingValue = "canway", matchIfMissing = true)
@Import(
    CanwayDepartmentServiceImpl::class
)
class DevopsAuthConfiguration
