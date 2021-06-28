package com.tencent.bkrepo.auth.listener.event.user

import com.tencent.bkrepo.auth.listener.event.IEvent
import com.tencent.bkrepo.repository.pojo.log.ResourceType

abstract class UserEvent(
    open val userIdData: String,
    open val nameData: String?,
    open val operator: String
) : IEvent(operator) {
    override fun getResourceType(): ResourceType = ResourceType.USER

    override fun getResourceKey(): String {
        val key = nameData?.let { ", name: $nameData" }
        return "userId:$userIdData$key"
    }

    override fun getRequest(): Map<String, Any> {
        return mapOf()
    }
}
