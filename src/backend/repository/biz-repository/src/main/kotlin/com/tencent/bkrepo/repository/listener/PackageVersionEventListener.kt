package com.tencent.bkrepo.repository.listener

import com.tencent.bkrepo.repository.listener.event.packageVersion.PackageVersionCreatedEvent
import com.tencent.bkrepo.repository.listener.event.packageVersion.PackageVersionDownloadEvent
import com.tencent.bkrepo.repository.listener.event.packageVersion.PackageVersionStagedEvent
import com.tencent.bkrepo.repository.listener.event.packageVersion.PackageVersionUpdatedEvent
import com.tencent.bkrepo.repository.listener.event.packageVersion.PackageVersionDeletedEvent
import com.tencent.bkrepo.repository.listener.event.packageVersion.PackageVersionEvent
import com.tencent.bkrepo.repository.pojo.bksoftware.DayMetricRequest
import com.tencent.bkrepo.repository.service.bksoftware.DayMetricService
import org.springframework.context.event.EventListener
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Component
import java.time.LocalDate

@Component
class PackageVersionEventListener(
    private val dayMetricService: DayMetricService
) : BaseEventListener() {

    @Async
    @EventListener(PackageVersionCreatedEvent::class)
    fun handle(event: PackageVersionCreatedEvent) {
        addDayMetric(event)
        logEvent(event)
    }

    @Async
    @EventListener(PackageVersionDeletedEvent::class)
    fun handle(event: PackageVersionDeletedEvent) {
        addDayMetric(event)
        logEvent(event)
    }

    @Async
    @EventListener(PackageVersionUpdatedEvent::class)
    fun handle(event: PackageVersionUpdatedEvent) {
        addDayMetric(event)
        logEvent(event)
    }

    @Async
    @EventListener(PackageVersionStagedEvent::class)
    fun handle(event: PackageVersionStagedEvent) {
        addDayMetric(event)
        logEvent(event)
    }

    @Async
    @EventListener(PackageVersionDownloadEvent::class)
    fun handle(event: PackageVersionDownloadEvent) {
        addDayMetric(event)
        logEvent(event)
    }

    private fun addDayMetric(event: PackageVersionEvent) {
        dayMetricService.add(
            DayMetricRequest(
                day = LocalDate.now(),
                projectId = event.projectId,
                repoName = event.repoName,
                type = event.getOperateType()
            )
        )
    }
}
