package com.tencent.bkrepo.common.metadata.listener

import com.tencent.bkrepo.common.artifact.event.project.ProjectDeletedEvent
import com.tencent.bkrepo.common.metadata.service.project.ProjectService
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import java.util.function.Consumer

@Component("devopsProjectDeleted")
class ProjectDeletedEventListener(
    private val projectService: ProjectService
) : Consumer<ProjectDeletedEvent> {

    override fun accept(event: ProjectDeletedEvent) {
        logger.info("Received project-deleted event: $event")
        projectService.deleteProject(event.userId, event.projectId, event.projectId)
    }

    companion object {
        private val logger = LoggerFactory.getLogger(ProjectDeletedEventListener::class.java)
    }
}
