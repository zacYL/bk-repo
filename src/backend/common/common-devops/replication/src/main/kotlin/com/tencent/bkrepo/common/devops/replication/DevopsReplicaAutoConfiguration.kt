package com.tencent.bkrepo.common.devops.replication

import com.tencent.bkrepo.common.devops.DEPLOY_CANWAY
import com.tencent.bkrepo.common.devops.replication.api.CanwayReplicaPermissionController
import com.tencent.bkrepo.common.devops.replication.service.CanwayReplicaPermissionService
import com.tencent.bkrepo.common.devops.service.BkUserService
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Import

@Configuration
@ConditionalOnProperty(prefix = "auth", name = ["realm"], havingValue = DEPLOY_CANWAY, matchIfMissing = true)
@Import(
    CanwayReplicaPermissionService::class,
    BkUserService::class,
    CanwayReplicaPermissionController::class
)
class DevopsReplicaAutoConfiguration
