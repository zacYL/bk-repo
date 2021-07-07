package com.tencent.bkrepo.auth.listener

import com.tencent.bkrepo.auth.listener.event.admin.AdminAddEvent
import com.tencent.bkrepo.auth.listener.event.admin.AdminDeleteEvent
import org.springframework.context.event.EventListener
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Component

@Component
class AdminEventListener : AbstractEventListener() {

    @Async
    @EventListener
    fun handle(event: AdminAddEvent) {
        logEvent(event)
    }

    @Async
    @EventListener
    fun handle(event: AdminDeleteEvent) {
        logEvent(event)
    }
}
