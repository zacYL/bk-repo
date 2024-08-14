package com.tencent.bkrepo.repository.listener

import com.tencent.bkrepo.common.artifact.event.project.ProjectUpdatedEvent
import com.tencent.bkrepo.repository.pojo.project.ProjectUpdateRequest
import com.tencent.bkrepo.repository.service.repo.ProjectService
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import java.util.function.Consumer

@Component("devopsProjectUpdated")
class ProjectUpdatedEventListener(
    private val projectService: ProjectService
) : Consumer<ProjectUpdatedEvent> {

    override fun accept(event: ProjectUpdatedEvent) {
        logger.info("Received project-updated event: $event")
        projectService.updateProject(event.projectId, ProjectUpdateRequest(displayName = event.projectName))
    }

    companion object {
        private val logger = LoggerFactory.getLogger(ProjectUpdatedEventListener::class.java)
    }
}
