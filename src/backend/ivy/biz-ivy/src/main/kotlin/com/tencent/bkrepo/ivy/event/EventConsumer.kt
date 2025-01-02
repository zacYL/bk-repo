package com.tencent.bkrepo.ivy.event

import com.tencent.bkrepo.common.artifact.event.base.ArtifactEvent
import com.tencent.bkrepo.common.artifact.event.base.EventType
import com.tencent.bkrepo.common.artifact.event.repo.RepositoryCleanEvent
import com.tencent.bkrepo.common.artifact.pojo.RepositoryType
import org.springframework.stereotype.Component
import java.util.function.Consumer

@Component("artifactEvent")
class EventConsumer (
) : Consumer<ArtifactEvent> {
    override fun accept(event: ArtifactEvent) {
        require(event.type == EventType.REPOSITORY_CLEAN) { return }
        with(event) {
            require(data[RepositoryCleanEvent::packageType.name] == RepositoryType.IVY) { return }
            val versionList = data[RepositoryCleanEvent::versionList.name] as List<String>
            versionList.forEach {
                
            }
        }
    }
}
