package com.tencent.bkrepo.repository.listener.event.packageVersion

import com.tencent.bkrepo.repository.listener.event.IEvent
import com.tencent.bkrepo.repository.pojo.log.ResourceType

abstract class PackageVersionEvent(
    open val projectId: String,
    open val repoName: String,
    open val packageKey: String,
    open val version: String?,
    open val operator: String
) : IEvent(operator) {
    override fun getResourceType(): ResourceType = ResourceType.PACKAGE

    override fun getResourceKey(): String {
        return "$packageKey:$version"
    }

    override fun getRequest(): Map<String, Any> {
        return mapOf("projectId" to projectId, "repoName" to repoName)
    }

}