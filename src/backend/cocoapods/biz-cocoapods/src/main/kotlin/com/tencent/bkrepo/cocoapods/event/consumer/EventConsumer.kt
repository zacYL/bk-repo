package com.tencent.bkrepo.cocoapods.event.consumer

import com.tencent.bkrepo.cocoapods.service.CocoapodsReplicaService
import com.tencent.bkrepo.cocoapods.service.CocoapodsSpecsService
import com.tencent.bkrepo.common.artifact.event.base.ArtifactEvent
import com.tencent.bkrepo.common.artifact.event.base.EventType
import com.tencent.bkrepo.common.artifact.event.repo.RepoCreatedEvent
import com.tencent.bkrepo.common.artifact.pojo.RepositoryCategory
import com.tencent.bkrepo.common.artifact.pojo.RepositoryType
import com.tencent.bkrepo.repository.api.RepositoryClient
import org.springframework.stereotype.Component
import java.util.function.Consumer

@Component("artifactEvent")
class EventConsumer (
    private val cocoapodsSpecsService: CocoapodsSpecsService,
    private val repositoryClient: RepositoryClient,
    private val cocoapodsReplicaService: CocoapodsReplicaService
) : Consumer<ArtifactEvent> {
    override fun accept(event: ArtifactEvent) {
        when (event.type) {
            EventType.COCOAPODS_REPLICA -> replicaHandle(event)
            EventType.REPO_CREATED -> repoCreateHandle(event)
            else -> {}
        }
    }
    private fun replicaHandle(event: ArtifactEvent) {
        cocoapodsReplicaService.resolveIndexFile(event)
    }

    private fun repoCreateHandle(event: ArtifactEvent) {
        logger.info("repo create event: $event")
        with(event){
            require(data[RepoCreatedEvent::repoType.name] == RepositoryType.COCOAPODS.name) { return }
            repositoryClient.getRepoInfo(projectId, repoName).data?.let { repoInfo ->
                when (repoInfo.category) {
                    RepositoryCategory.LOCAL -> {
                        cocoapodsSpecsService.initSpecs(projectId, repoName)
                    }

                    RepositoryCategory.REMOTE -> {
                        cocoapodsSpecsService.initRemoteSpecs(projectId, repoInfo)
                    }

                    else -> TODO()
                }
            }
        }
    }

    companion object {
        private val logger = org.slf4j.LoggerFactory.getLogger(EventConsumer::class.java)
    }
}
