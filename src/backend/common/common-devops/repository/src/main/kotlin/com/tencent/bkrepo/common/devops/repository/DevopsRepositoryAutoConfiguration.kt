package com.tencent.bkrepo.common.devops.repository

import com.tencent.bkrepo.common.devops.DEPLOY_CANWAY
import com.tencent.bkrepo.common.devops.repository.api.CanwayPermissionController
import com.tencent.bkrepo.common.devops.repository.permission.CanwayPermissionAspect
import com.tencent.bkrepo.common.devops.repository.service.CanwayPermissionService
import com.tencent.bkrepo.common.devops.service.BkUserService
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Import

@Configuration
@ConditionalOnProperty(prefix = "auth", name = ["realm"], havingValue = DEPLOY_CANWAY, matchIfMissing = true)
@Import(
    CanwayPermissionAspect::class,
    CanwayPermissionService::class,
    BkUserService::class,
    CanwayPermissionController::class
)
class DevopsRepositoryAutoConfiguration
