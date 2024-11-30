package com.tencent.bkrepo.auth.general

import com.tencent.bkrepo.auth.api.CanwayCustomPermissionClient
import com.tencent.bkrepo.auth.api.CanwayProjectClient
import com.tencent.bkrepo.auth.api.CanwaySystemClient
import com.tencent.bkrepo.auth.api.CanwayTenantClient
import com.tencent.bkrepo.auth.constant.AUTH_ADMIN
import com.tencent.bkrepo.auth.constant.AuthConstant.ANY_RESOURCE_CODE
import com.tencent.bkrepo.auth.constant.AuthConstant.SCOPECODE
import com.tencent.bkrepo.auth.pojo.enums.PermissionAction
import com.tencent.bkrepo.auth.pojo.enums.convertEnumListToStringList
import com.tencent.bkrepo.auth.pojo.general.ScopeDTO
import com.tencent.bkrepo.auth.pojo.permission.CustomPermissionQueryDTO
import com.tencent.bkrepo.auth.pojo.permission.PermissionVO
import com.tencent.bkrepo.auth.pojo.permission.RemoveInstancePermissionsRequest
import com.tencent.bkrepo.auth.pojo.permission.UserPermissionQueryDTO
import com.tencent.bkrepo.auth.pojo.permission.UserPermissionValidateDTO
import com.tencent.bkrepo.auth.pojo.role.SubjectDTO
import com.tencent.bkrepo.auth.service.impl.CanwayPermissionServiceImpl
import com.tencent.bkrepo.common.devops.REPO_PATH_RESOURCECODE
import com.tencent.bkrepo.common.devops.REPO_PATH_SCOPE_CODE
import com.tencent.bkrepo.common.devops.RESOURCECODE
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

@Component
class DevOpsAuthGeneral(
    private val canwayProjectClient: CanwayProjectClient,
    private val canwaySystemClient: CanwaySystemClient,
    private val canwayTenantClient: CanwayTenantClient,
    private val canwayCustomPermissionClient: CanwayCustomPermissionClient
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
        logger.info("Read Permission repoNameList:$repoList")

        if (repoList.contains(ANY_RESOURCE_CODE)) {
            repoList.remove(ANY_RESOURCE_CODE)
        }

        return repoList
    }

    /**
     * 获取项目下用户拥有各种权限的仓库列表
     */
    fun getUserActionPermission(projectId: String, userId: String, actions: List<PermissionAction>): List<String> {
        val repoList = mutableListOf<String>()
        canwayProjectClient.getUserPermission(
            projectId = projectId,
            option = UserPermissionQueryDTO(
                userId = userId,
                resourceCode = RESOURCECODE,
                actionCodes = convertEnumListToStringList(actions),
                paddingInstancePermission = true
            )
        ).data?.let {
            repoList.addAll(
                it.permissions
                    .filter { it.actionCodes.toSet() == convertEnumListToStringList(actions).toSet() }
                    .map { it.instanceId }
            )
        }
        logger.info("$actions Permission repoNameList:$repoList")

        if (repoList.contains(ANY_RESOURCE_CODE)) {
            repoList.remove(ANY_RESOURCE_CODE)
        }

        return repoList
    }

    /**
     * 删除实例权限数据
     */
    fun removeResourcePermissions(projectId: String, repoName: String): Boolean {
        return canwayCustomPermissionClient.removeResourcePermissions(
            userId = AUTH_ADMIN,
            request = RemoveInstancePermissionsRequest(
                instanceIds = listOf(repoName),
                resourceCode = RESOURCECODE,
                scope = Pair(SCOPECODE, projectId)
            )
        ).data ?: false
    }

    fun getRepoPathCollectPermission(
        userId: String,
        projectId: String,
        repoName: String,
        pathCollectionIds: List<String>
    ): List<PermissionVO> {
        // 查询用户关联的角色和权限作用域
        val subjects =
            canwayProjectClient.getUserRelatedRoleAndPermissionScope(userId, projectId).data?.map { it.first }
                ?: listOf(SubjectDTO.user(userId))
        return canwayCustomPermissionClient.queryPermission(
            CustomPermissionQueryDTO(
                scopes = listOf(ScopeDTO(REPO_PATH_SCOPE_CODE, "${projectId}_${repoName}")),
                subjects = subjects,
                instanceIds = pathCollectionIds,
                resourceCodes = listOf(REPO_PATH_RESOURCECODE)
            )
        ).data ?: emptyList()
    }

    companion object {
        val logger: Logger = LoggerFactory.getLogger(CanwayPermissionServiceImpl::class.java)
    }
}
