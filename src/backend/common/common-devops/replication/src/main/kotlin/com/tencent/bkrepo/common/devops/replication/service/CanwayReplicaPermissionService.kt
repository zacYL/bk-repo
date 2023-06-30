package com.tencent.bkrepo.common.devops.replication.service

import com.tencent.bkrepo.auth.api.CanwayProjectClient
import com.tencent.bkrepo.auth.constant.AuthConstant.ANY_RESOURCE_CODE
import com.tencent.bkrepo.auth.pojo.UserPermissionQueryDTO
import com.tencent.bkrepo.common.devops.REPLICA_RESOURCECODE
import com.tencent.bkrepo.common.devops.enums.CanwayPermissionType
import net.canway.devops.auth.pojo.UserPermissionValidateDTO
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class CanwayReplicaPermissionService(
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

    fun checkPermissionQuery(
        projectId: String,
        userId: String,
        resourceCode: String? = null,
        paddingInstancePermission: Boolean? = true
    ): List<String> {
        val replicaPermissionList = canwayProjectClient.getUserPermission(
            projectId = projectId,
            UserPermissionQueryDTO(
                userId = userId,
                resourceCode = resourceCode ?: REPLICA_RESOURCECODE,
                paddingInstancePermission = paddingInstancePermission ?: true
            )
        ).data?.permissions?.filter { it.instanceId == ANY_RESOURCE_CODE } ?: listOf()
        if (replicaPermissionList.isNotEmpty()) {
            return replicaPermissionList.flatMap { it.actionCodes }.distinct()
        }else{
            return listOf()
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
                resourceCode = resourceCode ?: REPLICA_RESOURCECODE,
                actionCodes = listOf(action.value)
            )
        ).data
    }

    companion object {
        val logger: Logger = LoggerFactory.getLogger(CanwayReplicaPermissionService::class.java)
    }
}
