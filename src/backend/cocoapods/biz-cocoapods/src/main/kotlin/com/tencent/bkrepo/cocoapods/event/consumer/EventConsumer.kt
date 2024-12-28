package com.tencent.bkrepo.cocoapods.event.consumer

import com.tencent.bkrepo.common.artifact.event.base.ArtifactEvent
import org.springframework.stereotype.Component
import java.util.function.Consumer

@Component("artifactEvent")
class EventConsumer (
    private val remoteEventJobExecutor: RemoteEventJobExecutor
) : Consumer<ArtifactEvent> {
    override fun accept(event: ArtifactEvent) {
        remoteEventJobExecutor.execute(event)
    }
}
