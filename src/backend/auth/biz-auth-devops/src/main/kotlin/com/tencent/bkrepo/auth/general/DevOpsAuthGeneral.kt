package com.tencent.bkrepo.auth.general

import com.tencent.bkrepo.auth.api.CanwayProjectClient
import com.tencent.bkrepo.auth.api.CanwaySystemClient
import com.tencent.bkrepo.auth.api.CanwayTenantClient
import com.tencent.bkrepo.auth.constant.AuthConstant.ANY_RESOURCE_CODE
import com.tencent.bkrepo.auth.pojo.UserPermissionQueryDTO
import com.tencent.bkrepo.auth.pojo.enums.PermissionAction
import com.tencent.bkrepo.auth.service.impl.CanwayPermissionServiceImpl
import com.tencent.bkrepo.common.devops.RESOURCECODE
import net.canway.devops.auth.pojo.UserPermissionValidateDTO
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

@Component
class DevOpsAuthGeneral(
    private val canwayProjectClient: CanwayProjectClient,
    private val canwaySystemClient: CanwaySystemClient,
    private val canwayTenantClient: CanwayTenantClient
) {
    /**
     * 判断用户是否为系统管理员
     */
    fun isSystemAdmin(userId: String): Boolean {
        return canwaySystemClient.isSystemAdmin(userId).data ?: false
    }

    /**
     * 判断用户是否为租户管理员
     */
    fun isTenantMemberOrAdmin(userId: String, tenantId: String): Boolean {
        return canwayTenantClient.isTenantMemberOrAdmin(userId, tenantId).data ?: false
    }

    /**
     * 判断用户是否为项目管理员
     */
    fun isProjectOrSuperiorAdmin(userId: String, projectId: String): Boolean {
        return canwayProjectClient.isProjectOrSuperiorAdmin(userId, projectId).data ?: false
    }

    /**
     * 判断用户是否为项目成员/项目管理员
     */

    fun isProjectMemberOrAdmin(userId: String, projectId: String): Boolean {
        return canwayProjectClient.isProjectMemberOrAdmin(projectId, userId).data ?: false
    }

    /**
     * 校验用户是否拥有项目指定资源权限
     */
    fun validateUserPermission(projectId: String, option: UserPermissionValidateDTO): Boolean {
        return canwayProjectClient.validateUserPermission(projectId, option).data ?: false
    }

    /**
     * 获取项目下用户拥有查看权限的仓库
     */
    fun getUserPermission(projectId: String, userId: String): List<String> {
        val repoList = mutableListOf<String>()
        canwayProjectClient.getUserPermission(
            projectId = projectId,
            option = UserPermissionQueryDTO(
                userId = userId,
                resourceCode = RESOURCECODE,
                actionCodes = listOf(PermissionAction.READ.name.toLowerCase()),
                paddingInstancePermission = true
            )
        ).data?.let { repoList.addAll(it.permissions.map { it.instanceId }) }
        logger.info("Read Permission repoNameList:${repoList}")

        if (repoList.contains(ANY_RESOURCE_CODE)) {
            repoList.remove(ANY_RESOURCE_CODE)
        }

        return repoList
    }

    companion object {
        val logger: Logger = LoggerFactory.getLogger(CanwayPermissionServiceImpl::class.java)
    }

}