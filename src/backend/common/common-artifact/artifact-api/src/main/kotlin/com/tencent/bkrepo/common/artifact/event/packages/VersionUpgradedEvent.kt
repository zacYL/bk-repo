package com.tencent.bkrepo.common.artifact.event.packages

import com.tencent.bkrepo.common.artifact.event.base.ArtifactEvent
import com.tencent.bkrepo.common.artifact.event.base.EventType

/**
 * 包版本晋级事件
 */
class VersionUpgradedEvent(
    override val projectId: String,
    override val repoName: String,
    override val userId: String,
    val packageKey: String,
    val packageVersion: String,
    val packageName: String,
    val packageType: String,
    val realIpAddress: String?,
    val oldStage: String,
    val newStage: String
) : ArtifactEvent(
    type = EventType.VERSION_STAGED,
    projectId = projectId,
    repoName = repoName,
    resourceKey = "$packageKey-$packageVersion",
    userId = userId,
    data = mutableMapOf(
        "packageKey" to packageKey,
        "packageType" to packageType,
        "packageName" to packageName,
        "packageVersion" to packageVersion,
        "oldStage" to oldStage,
        "newStage" to newStage
    ).apply {
        realIpAddress?.let { this["realIpAddress"] = realIpAddress }
    }
)
