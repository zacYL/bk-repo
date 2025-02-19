package com.tencent.bkrepo.replication.replica.replicator.standalone

import com.tencent.bkrepo.common.api.exception.NotFoundException
import com.tencent.bkrepo.common.api.message.CommonMessageCode
import com.tencent.bkrepo.common.service.condition.ConditionalOnDevops
import com.tencent.bkrepo.replication.config.ReplicationProperties
import com.tencent.bkrepo.replication.manager.LocalDataManager
import com.tencent.bkrepo.replication.replica.context.ReplicaContext
import com.tencent.bkrepo.replication.replica.replicator.base.internal.ClusterArtifactReplicationHandler
import org.springframework.stereotype.Component

@Component
@ConditionalOnDevops
class DevopsClusterReplicator(
    localDataManager: LocalDataManager,
    artifactReplicationHandler: ClusterArtifactReplicationHandler,
    replicationProperties: ReplicationProperties
) : ClusterReplicator(localDataManager, artifactReplicationHandler, replicationProperties) {

    override fun replicaProject(context: ReplicaContext) {
        with(context) {
            // 外部集群仓库没有project/repoName
            if (remoteProjectId.isNullOrBlank()) return
            // 平台侧管理项目时，由于无法确定租户，因此不自动创建项目
            if (artifactReplicaClient!!.checkProjectExist(remoteProjectId).data != true) {
                throw NotFoundException(CommonMessageCode.RESOURCE_NOT_FOUND, remoteProjectId)
            }
        }
    }
}
