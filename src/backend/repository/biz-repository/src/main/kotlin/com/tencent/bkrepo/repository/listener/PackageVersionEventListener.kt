package com.tencent.bkrepo.repository.listener

import com.tencent.bkrepo.repository.listener.event.packageVersion.PackageVersionCreatedEvent
import com.tencent.bkrepo.repository.listener.event.packageVersion.PackageVersionDeletedEvent
import com.tencent.bkrepo.repository.listener.event.packageVersion.PackageVersionStagedEvent
import com.tencent.bkrepo.repository.listener.event.packageVersion.PackageVersionUpdatedEvent
import com.tencent.bkrepo.repository.listener.event.packageVersion.PackageVersionDownloadEvent
import org.springframework.context.event.EventListener
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Component

@Component
class PackageVersionEventListener : BaseEventListener() {

    @Async
    @EventListener(PackageVersionCreatedEvent::class)
    fun handle(event: PackageVersionCreatedEvent) {
        logEvent(event)
    }

    @Async
    @EventListener(PackageVersionDeletedEvent::class)
    fun handle(event: PackageVersionDeletedEvent) {
        logEvent(event)
    }

    @Async
    @EventListener(PackageVersionUpdatedEvent::class)
    fun handle(event: PackageVersionUpdatedEvent) {
        logEvent(event)
    }

    @Async
    @EventListener(PackageVersionStagedEvent::class)
    fun handle(event: PackageVersionStagedEvent) {
        logEvent(event)
    }

    @Async
    @EventListener(PackageVersionDownloadEvent::class)
    fun handle(event: PackageVersionDownloadEvent) {
        logEvent(event)
    }
}
