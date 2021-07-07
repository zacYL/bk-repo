package com.tencent.bkrepo.auth.listener.event.admin

import com.tencent.bkrepo.auth.listener.event.IEvent
import com.tencent.bkrepo.common.api.util.toJsonString
import com.tencent.bkrepo.repository.pojo.log.ResourceType

abstract class AdminEvent(
    open val list: List<String>,
    open val operator: String
) : IEvent(operator) {
    override fun getResourceType(): ResourceType = ResourceType.ADMIN

    override fun getResourceKey(): String {
        return list.toJsonString()
    }

    override fun getRequest(): Map<String, Any> {
        return mapOf("list" to list)
    }
}
