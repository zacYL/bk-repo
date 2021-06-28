package com.tencent.bkrepo.auth.listener

import com.tencent.bkrepo.auth.listener.event.user.UserCreateEvent
import org.springframework.context.event.EventListener
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Component

@Component
class UserEventListener : AbstractEventListener() {

    @Async
    @EventListener
    fun handle(event: UserCreateEvent) {
        logEvent(event)
    }
}
