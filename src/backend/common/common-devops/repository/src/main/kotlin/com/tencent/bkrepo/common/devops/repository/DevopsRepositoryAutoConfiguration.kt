package com.tencent.bkrepo.common.devops.repository

import com.tencent.bkrepo.common.devops.api.conf.CanwayMailConf
import com.tencent.bkrepo.common.devops.repository.aspect.DeployAspectConf
import com.tencent.bkrepo.common.devops.repository.permission.CanwayPermissionAspect
import com.tencent.bkrepo.common.devops.api.service.BkUserService
import com.tencent.bkrepo.common.devops.repository.api.CanwayPermissionController
import com.tencent.bkrepo.common.devops.repository.service.CanwayPermissionService
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Import

@Configuration
@ConditionalOnProperty(prefix = "deploy", name = ["mode"], havingValue = "devops", matchIfMissing = true)
@Import(
    CanwayPermissionAspect::class,
    CanwayPermissionService::class,
    CanwayMailConf::class,
    DeployAspectConf::class,
    BkUserService::class,
    CanwayPermissionController::class
)
class DevopsRepositoryAutoConfiguration
