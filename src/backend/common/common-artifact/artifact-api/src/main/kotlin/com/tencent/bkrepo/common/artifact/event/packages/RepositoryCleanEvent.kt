package com.tencent.bkrepo.common.artifact.event.packages

import com.tencent.bkrepo.common.artifact.event.base.ArtifactEvent
import com.tencent.bkrepo.common.artifact.event.base.EventType

class RepositoryCleanEvent (
    override val projectId: String,
    override val repoName: String,
    override val userId: String,
    val packageKey: String,
    val packageVersion: String,
    val packageType: String,
    val realIpAddress: String?
) : ArtifactEvent(
    type = EventType.REPOSITORY_CLEAN,
    projectId = projectId,
    repoName = repoName,
    resourceKey = "$packageKey-$packageVersion",
    userId = userId,
    data = mutableMapOf(
        "packageKey" to packageKey,
        "packageType" to packageType,
        "packageVersion" to packageVersion
    ).apply {
        realIpAddress?.let { this["realIpAddress"] = realIpAddress }
    }
)
