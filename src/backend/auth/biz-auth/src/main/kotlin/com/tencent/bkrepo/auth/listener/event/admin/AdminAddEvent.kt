package com.tencent.bkrepo.auth.listener.event.admin

import com.tencent.bkrepo.auth.listener.event.AuthEvent
import com.tencent.bkrepo.common.artifact.event.base.EventType

class AdminAddEvent(
    override val type: EventType = EventType.ADMIN_ADD,
    override val resourceKey: String,
    override val userId: String
) : AuthEvent(type = type, resourceKey = resourceKey, userId = userId)
