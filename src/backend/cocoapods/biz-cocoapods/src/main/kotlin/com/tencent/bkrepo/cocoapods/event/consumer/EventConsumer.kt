package com.tencent.bkrepo.cocoapods.event.consumer

import com.tencent.bkrepo.common.artifact.event.base.ArtifactEvent
import com.tencent.bkrepo.common.artifact.event.base.EventType
import org.springframework.stereotype.Component
import java.util.function.Consumer

@Component("artifactEvent")
class EventConsumer (
    private val remoteEventJobExecutor: RemoteEventJobExecutor
) : Consumer<ArtifactEvent> {
    companion object {
        var supportEvent = listOf(EventType.REPO_CREATED, EventType.COCOAPODS_REPLICA)
    }

    override fun accept(event: ArtifactEvent) {
        if (supportEvent.contains(event.type)) {
            remoteEventJobExecutor.execute(event)
        }
    }
}
