package com.tencent.bkrepo.npm.controller

import com.tencent.bkrepo.common.artifact.event.base.ArtifactEvent
import com.tencent.bkrepo.common.security.permission.Principal
import com.tencent.bkrepo.common.security.permission.PrincipalType
import com.tencent.bkrepo.npm.constants.NPM_REPLICA_RESOLVE
import com.tencent.bkrepo.npm.service.NpmReplicaService
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController

@Principal(type = PrincipalType.ADMIN)
@RestController
class NpmReplicationController(
    private val npmReplicaService: NpmReplicaService,
) {
    /**
     * 处理制品分发事件
     */
    @PostMapping(NPM_REPLICA_RESOLVE)
    fun resolve(
        @RequestBody event: ArtifactEvent
    ) {
        npmReplicaService.resolveIndexFile(event)
    }
}
