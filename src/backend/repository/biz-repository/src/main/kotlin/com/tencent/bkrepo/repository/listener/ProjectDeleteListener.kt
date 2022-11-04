package com.tencent.bkrepo.repository.listener

import com.tencent.bkrepo.common.artifact.event.project.ProjectDeletedEvent
import com.tencent.bkrepo.repository.service.repo.ProjectService
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import java.util.function.Consumer

@Component("devopsEvent")
class ProjectDeleteListener(
    private val projectService: ProjectService
) : Consumer<ProjectDeletedEvent> {

    override fun accept(event: ProjectDeletedEvent) {
        logger.info("Receive project delete event: $event")
        projectService.deleteProject(event.userId, event.projectId, event.projectId)
    }

    companion object {
        private val logger = LoggerFactory.getLogger(ProjectDeleteListener::class.java)
    }
}
