package com.tencent.bkrepo.common.devops.repository.service

import com.tencent.bkrepo.auth.api.CanwayProjectClient
import com.tencent.bkrepo.auth.constant.AuthConstant.ANY_RESOURCE_CODE
import com.tencent.bkrepo.common.devops.RESOURCECODE
import com.tencent.bkrepo.common.devops.enums.CanwayPermissionType
import com.tencent.bkrepo.auth.pojo.permission.UserPermissionValidateDTO
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class CanwayPermissionService(
    private val canwayProjectClient: CanwayProjectClient,
) {
    @Suppress("TooGenericExceptionCaught")
    fun checkCanwayPermission(
        projectId: String,
        repoName: String?,
        userId: String,
        action: CanwayPermissionType
    ): Boolean {
        return try {
            checkPermissionInstance(
                projectId = projectId,
                userId = userId,
                action = action
            ) ?: false
        } catch (e: Exception) {
            logger.error("Devops permission request failed: ", e)
            false
        }
    }

    fun checkPermissionInstance(
        projectId: String,
        userId: String,
        action: CanwayPermissionType,
        resourceCode: String? = null,
        repoName: String? = null
    ): Boolean? {
        return canwayProjectClient.validateUserPermission(
            projectId = projectId,
            option = UserPermissionValidateDTO(
                userId = userId,
                instanceId = repoName ?: ANY_RESOURCE_CODE,
                resourceCode = resourceCode ?: RESOURCECODE,
                actionCodes = listOf(action.value)
            )
        ).data
    }

    companion object {
        val logger: Logger = LoggerFactory.getLogger(CanwayPermissionService::class.java)
    }
}
