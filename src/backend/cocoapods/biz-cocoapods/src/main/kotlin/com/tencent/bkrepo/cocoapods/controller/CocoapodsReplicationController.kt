package com.tencent.bkrepo.cocoapods.controller;

import com.tencent.bkrepo.cocoapods.service.CocoapodsReplicaService
import com.tencent.bkrepo.common.artifact.event.base.ArtifactEvent;
import com.tencent.bkrepo.cocoapods.constant.COCOAPODS_REPLICA_RESOLVE
import com.tencent.bkrepo.cocoapods.event.consumer.RemoteEventJobExecutor
import com.tencent.bkrepo.common.security.permission.Principal
import com.tencent.bkrepo.common.security.permission.PrincipalType
import org.slf4j.LoggerFactory
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
@Principal(type = PrincipalType.ADMIN)
@RestController
class CocoapodsReplicationController (
    private val cocoapodsReplicaService: CocoapodsReplicaService
){
    /**
     * 将修改索引文件暴露给外部
     * 外部通过http进行调用
     */
    @PostMapping(COCOAPODS_REPLICA_RESOLVE)
    fun resolve(@RequestBody event:ArtifactEvent,
                @PathVariable projectId: String,
                @PathVariable repoName: String,){
        logger.info("resolveIndexFile form http:projectId:$projectId, repoName:$repoName")
        cocoapodsReplicaService.resolveIndexFile(event)
    }
    companion object {
        private val logger = LoggerFactory.getLogger(RemoteEventJobExecutor::class.java)
    }
}
