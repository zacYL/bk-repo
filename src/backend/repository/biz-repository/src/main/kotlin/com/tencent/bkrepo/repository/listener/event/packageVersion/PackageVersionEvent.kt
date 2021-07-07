package com.tencent.bkrepo.repository.listener.event.packageVersion

import com.tencent.bkrepo.repository.listener.event.IEvent
import com.tencent.bkrepo.repository.pojo.log.ResourceType
import com.tencent.bkrepo.repository.pojo.packages.PackageType

abstract class PackageVersionEvent(
    open val projectId: String,
    open val repoName: String,
    open val repoType: PackageType,
    open val packageKey: String,
    open val packageName: String,
    open val packageVersion: String?,
    open val operator: String
) : IEvent(operator) {
    override fun getResourceType(): ResourceType = ResourceType.PACKAGE

    override fun getResourceKey(): String {
        return "$packageKey:$packageVersion"
    }

    override fun getRequest(): Map<String, Any> {
        return mapOf(
            "projectId" to projectId,
            "repoName" to repoName,
            "repoType" to repoType,
            "packageName" to packageName,
            "packageVersion" to (packageVersion ?: "")
        )
    }
}
