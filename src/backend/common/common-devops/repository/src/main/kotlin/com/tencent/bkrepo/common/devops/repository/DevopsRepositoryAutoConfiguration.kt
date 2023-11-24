package com.tencent.bkrepo.common.devops.repository

import com.tencent.bkrepo.common.devops.DEPLOY_CANWAY
import com.tencent.bkrepo.common.devops.repository.api.CanwayProjectController
import com.tencent.bkrepo.common.devops.repository.service.CanwayProjectService
import com.tencent.bkrepo.common.devops.service.BkUserService
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Import

@Configuration
@ConditionalOnProperty(prefix = "auth", name = ["realm"], havingValue = DEPLOY_CANWAY, matchIfMissing = true)
@Import(
    CanwayProjectService::class,
    BkUserService::class,
    CanwayProjectController::class,
)
class DevopsRepositoryAutoConfiguration
