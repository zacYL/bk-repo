package com.tencent.bkrepo.cocoapods.event

import com.tencent.bkrepo.cocoapods.service.CocoapodsFileService
import com.tencent.bkrepo.common.artifact.event.base.ArtifactEvent
import com.tencent.bkrepo.common.artifact.event.base.EventType
import com.tencent.bkrepo.common.artifact.event.repo.RepoCreatedEvent
import com.tencent.bkrepo.common.artifact.pojo.RepositoryType
import org.springframework.stereotype.Component
import java.util.function.Consumer

@Component("artifactEvent")
class EventConsumer (
    private val cocoapodsFileService: CocoapodsFileService
) : Consumer<ArtifactEvent> {
    override fun accept(event: ArtifactEvent) {
        require(event.type == EventType.REPO_CREATED) { return }
        event as RepoCreatedEvent
        with(event) {
            require(repoType == RepositoryType.COCOAPODS) { return }
            cocoapodsFileService.initSpecs(projectId,repoName)
        }
    }
}
