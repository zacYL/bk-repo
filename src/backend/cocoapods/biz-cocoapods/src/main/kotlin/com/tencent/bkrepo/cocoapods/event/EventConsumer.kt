package com.tencent.bkrepo.cocoapods.event

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
    private val repositoryClient: RepositoryClient
) : Consumer<ArtifactEvent> {
    override fun accept(event: ArtifactEvent) {
        if (event.type == EventType.REPO_CREATED){
            repoCreateHandle(event)
        }
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
