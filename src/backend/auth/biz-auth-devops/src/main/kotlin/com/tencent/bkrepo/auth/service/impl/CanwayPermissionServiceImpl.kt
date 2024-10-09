package com.tencent.bkrepo.auth.service.impl

import com.tencent.bkrepo.auth.constant.AUTH_BUILTIN_ADMIN
import com.tencent.bkrepo.auth.constant.AUTH_BUILTIN_USER
import com.tencent.bkrepo.auth.constant.AuthConstant.ANY_RESOURCE_CODE
import com.tencent.bkrepo.auth.general.DevOpsAuthGeneral
import com.tencent.bkrepo.auth.message.AuthMessageCode
import com.tencent.bkrepo.auth.pojo.RegisterResourceRequest
import com.tencent.bkrepo.auth.pojo.enums.PermissionAction
import com.tencent.bkrepo.auth.pojo.enums.ResourceType
import com.tencent.bkrepo.auth.pojo.permission.Permission
import com.tencent.bkrepo.auth.pojo.permission.CheckPermissionRequest
import com.tencent.bkrepo.auth.pojo.permission.CreatePermissionRequest
import com.tencent.bkrepo.auth.pojo.permission.UpdatePermissionPathRequest
import com.tencent.bkrepo.auth.pojo.permission.UpdatePermissionRepoRequest
import com.tencent.bkrepo.auth.repository.PermissionRepository
import com.tencent.bkrepo.auth.repository.RoleRepository
import com.tencent.bkrepo.auth.repository.UserRepository
import com.tencent.bkrepo.common.api.exception.ErrorCodeException
import com.tencent.bkrepo.common.devops.REPLICA_RESOURCECODE
import com.tencent.bkrepo.common.devops.RESOURCECODE
import com.tencent.bkrepo.repository.api.ProjectClient
import com.tencent.bkrepo.repository.api.RepositoryClient
import com.tencent.bkrepo.auth.pojo.permission.UserPermissionValidateDTO
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.data.mongodb.core.MongoTemplate

class CanwayPermissionServiceImpl(
    private val userRepository: UserRepository,
    private val roleRepository: RoleRepository,
    private val permissionRepository: PermissionRepository,
    private val devOpsAuthGeneral: DevOpsAuthGeneral,
    mongoTemplate: MongoTemplate,
    repositoryClient: RepositoryClient,
    projectClient: ProjectClient
) : CpackPermissionServiceImpl(
    userRepository,
    roleRepository,
    permissionRepository,
    mongoTemplate,
    repositoryClient,
    projectClient
) {
    /**
     * 删除权限方法（无法删除，因集成环境下不再保存制品库权限）
     */
    override fun deletePermission(id: String): Boolean {
        logger.info("Cpack permission does not exist, devops platform cannot delete permission")
        return true
    }

    /**
     * 权限列表
     */
    override fun listPermission(projectId: String, repoName: String?): List<Permission> {
        logger.debug("list  permission  projectId: [$projectId], repoName: [$repoName]")
        repoName?.let {
            return permissionRepository.findByResourceTypeAndProjectIdAndRepos(ResourceType.REPO, projectId, repoName)
                .map { transferPermission(it) }
        }
        return permissionRepository.findByResourceTypeAndProjectId(ResourceType.PROJECT, projectId)
            .map { transferPermission(it) }
    }

    /**
     * 根据制品库资源类型进行分类处理
     * 优先判断用户是否为超管
     * 系统类型->判断是否为超管
     * 项目类型->判断是否为项目成员/项目管理员
     * 仓库类型-管理员权限->判断是否为项目管理员
     * 其他类型（仓库与节点）->判断制品仓库具体权限
     */
    override fun checkPermission(request: CheckPermissionRequest): Boolean {
        try {
            if (devOpsAuthGeneral.isSystemAdmin(request.uid)) {
                return true
            }
            return with(request) {
                when (resourceType) {
                    ResourceType.SYSTEM -> {
                        true
                    }

                    ResourceType.PROJECT -> {
                        when (action) {
                            PermissionAction.MANAGE -> {
                                devOpsAuthGeneral.isProjectOrSuperiorAdmin(uid, projectId!!)
                            }
                            PermissionAction.READ -> {
                                devOpsAuthGeneral.isProjectMemberOrAdmin(uid, projectId!!)
                            }
                            else -> {
                                devOpsAuthGeneral.validateUserPermission(
                                    projectId = projectId!!,
                                    option = UserPermissionValidateDTO(
                                        userId = uid,
                                        instanceId = ANY_RESOURCE_CODE,
                                        resourceCode = RESOURCECODE,
                                        actionCodes = listOf(action.toString().toLowerCase())
                                    )
                                )
                            }
                        }
                    }

                    else -> {
                        devOpsAuthGeneral.validateUserPermission(
                            projectId = projectId!!,
                            option = UserPermissionValidateDTO(
                                userId = uid,
                                instanceId = repoName ?: ANY_RESOURCE_CODE,
                                resourceCode = if (request.resourceType == ResourceType.REPLICATION) {
                                    REPLICA_RESOURCECODE
                                } else {
                                    RESOURCECODE
                                },
                                actionCodes = listOf(action.toString().toLowerCase())
                            )
                        )
                    }
                }
            }
        } catch (exception: Exception) {
            logger.error("Devops permission request failed: ", exception)
            throw ErrorCodeException(AuthMessageCode.AUTH_PERMISSION_FAILED)
        }
    }

    /**
     * 增加权限方法（无法增加，因集成环境下不再保存制品库权限）
     */
    override fun createPermission(request: CreatePermissionRequest): Boolean {
        TODO("Not yet implemented")
    }

    /**
     * 无法更新权限include path
     */
    override fun updateIncludePath(request: UpdatePermissionPathRequest): Boolean {
        TODO("Not yet implemented")
    }

    /**
     * 无法更新权限exclude path
     */
    override fun updateExcludePath(request: UpdatePermissionPathRequest): Boolean {
        TODO("Not yet implemented")
    }

    /**
     * 无法更新权限权限绑定repo
     */
    override fun updateRepoPermission(request: UpdatePermissionRepoRequest): Boolean {
        TODO("Not yet implemented")
    }

    /**
     * 无需注册资源
     */
    override fun registerResource(request: RegisterResourceRequest) {
        TODO("Not yet implemented")
    }

    /**
     * 获取仓库内置权限列表
     */
    override fun listBuiltinPermission(projectId: String, repoName: String): List<Permission> {
        val repoAdmin = getOnePermission(projectId, repoName, AUTH_BUILTIN_ADMIN, listOf(PermissionAction.MANAGE))
        val repoUser = getOnePermission(
            projectId,
            repoName,
            AUTH_BUILTIN_USER,
            listOf(PermissionAction.WRITE, PermissionAction.DELETE, PermissionAction.UPDATE)
        )
        return listOf(repoAdmin, repoUser).map { transferPermission(it) }
    }

    /**
     * 判断是否为DevOps管理员
     */
    override fun isAdmin(userId: String, projectId: String?, tenantId: String?): Boolean {
        return isCIAdmin(userId = userId, projectId = projectId, tenantId = tenantId)
    }

    /**
     * 删除权限中心数据
     */
    override fun deletePermissionData(projectId: String, repoName: String): Boolean {
        return devOpsAuthGeneral.removeResourcePermissions(projectId, repoName)
    }

    /**
     * 用户可查看的有权限项目
     */
    override fun listPermissionProject(userId: String): List<String> {
        logger.debug("list permission project request : $userId ")
        if (userId.isEmpty()) return emptyList()
        val user = userRepository.findFirstByUserId(userId) ?: run {
            return listOf()
        }

        // 用户为CI 管理员
        if (isCIAdmin(userId)) {
            return projectClient.listProject().data?.map { it.name } ?: emptyList()
        }

        val projectList = mutableListOf<String>()

        // 非管理员用户关联权限
        projectList.addAll(getNoAdminUserProject(userId))

        if (user.roles.isEmpty()) {
            return projectList.distinct()
        }

        val noAdminRole = mutableListOf<String>()

        // 管理员角色关联权限
        val roleList = roleRepository.findByIdIn(user.roles)
        roleList.forEach {
            if (it.admin && it.projectId != null) {
                projectList.add(it.projectId!!)
            } else {
                noAdminRole.add(it.id!!)
            }
        }

        // 非管理员角色关联权限
        projectList.addAll(getNoAdminRoleProject(noAdminRole))

        return projectList.distinct()
    }

    /**
     * 用户可查看的有权限仓库
     * 1、管理员可查看全部；
     * 2、公开仓库全部人都可查看；
     * 3、判断查询是否为”上传“权限仓库列表，反之为查询“访问”权限与公开仓库列表；
     */
    override fun listPermissionRepo(
        projectId: String,
        userId: String,
        appId: String?,
        actions: List<PermissionAction>?
    ): List<String> {
        logger.debug("list repo permission request : [$projectId, $userId] ")

        // 用户是否为DevOps管理员
        if (isCIAdmin(userId, projectId)) {
            return getAllRepoByProjectId(projectId)
        }
        val repoList = mutableListOf<String>()

        if (actions == null || (actions.size == 1 && actions.firstOrNull() == PermissionAction.READ)) {
            // 获取系统内公开于匿名公开仓库名称
            repoList.addAll(listPublicRepo(projectId))
            // 获取该用户可查看的所有制品库仓库名称
            repoList.addAll(devOpsAuthGeneral.getUserPermission(projectId, userId))
            logger.info("repoList:$repoList")
        } else {
            repoList.addAll(devOpsAuthGeneral.getUserActionPermission(projectId, userId, actions))
            logger.info("repoList:$repoList")
        }

        return repoList.distinct()
    }

    /**
     * 用户是否为CI 管理员
     * @param userId: 用户id
     * @param projectId: 项目id
     * @param tenantId: 租户id
     */
    @Suppress("TooGenericExceptionCaught")
    private fun isCIAdmin(userId: String, projectId: String? = null, tenantId: String? = null): Boolean {
        logger.info("check user $userId is CI admin")
        if (devOpsAuthGeneral.isSystemAdmin(userId)) {
            return true
        }
        return if (projectId != null && tenantId == null) {
            logger.info("check user $userId is CI project admin of [project: $projectId]")
            devOpsAuthGeneral.isProjectOrSuperiorAdmin(userId, projectId)
        } else if (projectId == null && tenantId != null) {
            logger.info("check user $userId is CI tenant admin of [tenant: $tenantId]")
            devOpsAuthGeneral.isTenantMemberOrAdmin(userId, tenantId)
        } else {
            return false
        }
    }

    companion object {
        val logger: Logger = LoggerFactory.getLogger(CanwayPermissionServiceImpl::class.java)
    }
}
