package com.tencent.bkrepo.repository.cpack.service.impl

import com.tencent.bkrepo.common.api.constant.ADMIN_USER
import com.tencent.bkrepo.common.api.constant.StringPool.EMPTY
import com.tencent.bkrepo.common.artifact.constant.PAAS_PROJECT
import com.tencent.bkrepo.common.artifact.constant.PUBLIC_GLOBAL_PROJECT
import com.tencent.bkrepo.common.devops.client.CanwayProjectManagerClient
import com.tencent.bkrepo.common.devops.pojo.project.ProjectCreateRequest
import com.tencent.bkrepo.repository.api.ProjectClient
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class CanwayProjectService(
    private val projectClient: ProjectClient,
    private val canwayProjectManagerClient: CanwayProjectManagerClient
) {
    @Suppress("TooGenericExceptionCaught")
    fun canwayProjectMigrate(userId: String, tenantId: String, issueId: String) {
        // 查询制品库的项目列表
        val projectList = projectClient.listProject().data
        // 查询DevOps平台的项目列表
        val cwProjectIdList = canwayProjectManagerClient.getAllProject(userId).data?.map { it.projectCode }
        // 判断是否存在，不存在则创建
        projectList?.forEach { project ->
            if (project.name == PUBLIC_GLOBAL_PROJECT || project.name == PAAS_PROJECT) return@forEach
            if (cwProjectIdList?.contains(project.name) != true) {
                canwayProjectManagerClient.createProject(
                    userId = userId,
                    tenantId = tenantId,
                    projectCreateRequest = ProjectCreateRequest(
                        typeId = "2",
                        parentCode = EMPTY,
                        projectName = project.displayName,
                        englishNameCustom = project.name,
                        description = project.description,
                        deptId = EMPTY,
                        deptName = EMPTY,
                        relationProject = EMPTY,
                        relationDemand = EMPTY,
                        administrator = ADMIN_USER,
                        roleTemplates = EMPTY,
                        props = listOf(),
                        templateId = issueId,
                        projectCode = project.name
                    )
                )
                logger.info("[${project.displayName}] project create successfully ")
            } else {
                logger.info("[${project.displayName}] project already exists")
            }
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(CanwayProjectService::class.java)
    }
}
