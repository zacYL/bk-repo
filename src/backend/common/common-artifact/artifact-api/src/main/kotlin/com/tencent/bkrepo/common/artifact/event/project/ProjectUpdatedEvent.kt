package com.tencent.bkrepo.common.artifact.event.project

import com.tencent.bkrepo.common.api.constant.StringPool
import com.tencent.bkrepo.common.artifact.event.base.ArtifactEvent
import com.tencent.bkrepo.common.artifact.event.base.EventType

/**
 * 项目更新事件
 */
data class ProjectUpdatedEvent(
    override val projectId: String,
    override val userId: String,
    val projectName: String
) : ArtifactEvent(
    type = EventType.PROJECT_UPDATED,
    projectId = projectId,
    repoName = StringPool.EMPTY,
    resourceKey = projectId,
    userId = userId,
    data = mapOf("projectName" to projectName)
)
