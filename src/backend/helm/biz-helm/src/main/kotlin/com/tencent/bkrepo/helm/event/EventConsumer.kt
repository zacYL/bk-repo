package com.tencent.bkrepo.helm.event

import com.tencent.bkrepo.common.artifact.event.base.ArtifactEvent
import com.tencent.bkrepo.common.artifact.event.base.EventType
import com.tencent.bkrepo.common.artifact.event.packages.VersionDeletedEvent
import com.tencent.bkrepo.helm.constants.REPO_TYPE
import com.tencent.bkrepo.helm.service.ServiceHelmClientService
import com.tencent.bkrepo.repository.constant.SYSTEM_USER
import org.springframework.stereotype.Component
import java.util.function.Consumer

@Component("artifactEvent")
class EventConsumer(
    private val service: ServiceHelmClientService
) : Consumer<ArtifactEvent> {
    override fun accept(event: ArtifactEvent) {
        require(event.type == EventType.REPOSITORY_CLEAN) { return }
        with(event) {
            require(data[VersionDeletedEvent::packageType.name] == REPO_TYPE) { return }
            service.deleteVersion(
                projectId,
                repoName,
                data[VersionDeletedEvent::packageKey.name] as String,
                data[VersionDeletedEvent::packageVersion.name] as String,
                SYSTEM_USER
            )
        }
    }
}
