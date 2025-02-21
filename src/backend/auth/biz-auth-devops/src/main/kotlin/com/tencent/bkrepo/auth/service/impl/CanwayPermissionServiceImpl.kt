package com.tencent.bkrepo.auth.service.impl

import com.tencent.bkrepo.auth.constant.AUTH_BUILTIN_ADMIN
import com.tencent.bkrepo.auth.constant.AUTH_BUILTIN_USER
import com.tencent.bkrepo.auth.constant.AuthConstant.ANY_RESOURCE_CODE
import com.tencent.bkrepo.auth.dao.AccountDao
import com.tencent.bkrepo.auth.dao.PermissionDao
import com.tencent.bkrepo.auth.dao.PersonalPathDao
import com.tencent.bkrepo.auth.dao.RepoAuthConfigDao
import com.tencent.bkrepo.auth.dao.UserDao
import com.tencent.bkrepo.auth.dao.repository.RoleRepository
import com.tencent.bkrepo.auth.general.DevOpsAuthGeneral
import com.tencent.bkrepo.auth.message.AuthMessageCode
import com.tencent.bkrepo.auth.model.TPermission
import com.tencent.bkrepo.auth.pojo.enums.PermissionAction
import com.tencent.bkrepo.auth.pojo.enums.ResourceType
import com.tencent.bkrepo.auth.pojo.permission.Permission
import com.tencent.bkrepo.auth.pojo.permission.CheckPermissionRequest
import com.tencent.bkrepo.auth.pojo.permission.CreatePermissionRequest
import com.tencent.bkrepo.auth.pojo.permission.UpdatePermissionPathRequest
import com.tencent.bkrepo.auth.pojo.permission.UpdatePermissionRepoRequest
import com.tencent.bkrepo.common.api.exception.ErrorCodeException
import com.tencent.bkrepo.common.devops.REPLICA_RESOURCECODE
import com.tencent.bkrepo.common.devops.RESOURCECODE
import com.tencent.bkrepo.auth.pojo.permission.UserPermissionValidateDTO
import com.tencent.bkrepo.auth.util.request.PermRequestUtil
import com.tencent.bkrepo.common.artifact.path.PathUtils
import com.tencent.bkrepo.common.metadata.service.project.ProjectService
import com.tencent.bkrepo.common.metadata.service.repo.RepositoryService
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.time.LocalDateTime
import java.util.stream.Collectors

class CanwayPermissionServiceImpl(
    accountDao: AccountDao,
    private val userDao: UserDao,
    private val roleRepository: RoleRepository,
    private val permissionDao: PermissionDao,
    personalPathDao: PersonalPathDao,
    repoAuthConfigDao: RepoAuthConfigDao,
    private val devOpsAuthGeneral: DevOpsAuthGeneral,
    repositoryClient: RepositoryService,
    projectClient: ProjectService
) : CpackPermissionServiceImpl(
    accountDao,
    userDao,
    roleRepository,
    permissionDao,
    personalPathDao,
    repoAuthConfigDao,
    repositoryClient,
    projectClient
) {

    /**
     * 权限列表
     */
    override fun listPermission(projectId: String, repoName: String?): List<Permission> {
        logger.debug("list  permission  projectId: [$projectId], repoName: [$repoName]")
        repoName?.let {
            return permissionDao.listByResourceAndRepo(ResourceType.REPO, projectId, repoName)
                .map { PermRequestUtil.convToPermission(it) }
        }
        return permissionDao.listByResourceAndProject(ResourceType.PROJECT, projectId)
            .map { PermRequestUtil.convToPermission(it) }
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
                                        actionCodes = listOf(action.id())
                                    )
                                )
                            }
                        }
                    }

                    ResourceType.NODE -> {
                        checkNodePermission(request)
                    }

                    ResourceType.REPO -> {

                        val validateUserPermission = devOpsAuthGeneral.validateUserPermission(
                            projectId = projectId!!,
                            option = UserPermissionValidateDTO(
                                userId = uid,
                                instanceId = repoName ?: ANY_RESOURCE_CODE,
                                resourceCode = if (request.resourceType == ResourceType.REPLICATION) {
                                    REPLICA_RESOURCECODE
                                } else {
                                    RESOURCECODE
                                },
                                actionCodes = listOf(action.id())
                            )
                        )
                        if (!validateUserPermission && action == PermissionAction.READ) {
                            // 仓库权限校验失败且是校验read权限，则拥有路径read权限也有仓库的read权限
                            val repoPathCollectPermission = devOpsAuthGeneral.getRepoPathCollectPermission(
                                uid,
                                projectId!!,
                                listOf(repoName!!),
                                emptyList()
                            )
                            val pathReadPermission =
                                repoPathCollectPermission.filter {
                                    it.actionCode == PermissionAction.READ.id()
                                }
                            return pathReadPermission.isNotEmpty()
                        } else {
                            validateUserPermission
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
                                actionCodes = listOf(action.id())
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

    private fun checkNodePermission(request: CheckPermissionRequest): Boolean {
        with(request) {
            if (devOpsAuthGeneral.isProjectOrSuperiorAdmin(request.uid, projectId!!)) {
                return true
            }

            // 先校验仓库是否有这个权限
            val hasRepositoryPermission = devOpsAuthGeneral.validateUserPermission(
                projectId = projectId!!,
                option = UserPermissionValidateDTO(
                    userId = uid,
                    instanceId = repoName!!,
                    resourceCode = RESOURCECODE,
                    actionCodes = listOf(action.id())
                )
            )
            if (hasRepositoryPermission) {
                return true
            }

            if (path.isNullOrEmpty()) return true

            // 查询出当前路径所配置路径集合
            val pathCollections = permissionDao.listByResourceAndRepo(ResourceType.NODE, projectId!!, repoName!!)
            val pathSet = setOf(path)
            val pathToPathCollections = pathSet.map { requestPath ->
                val collections = pathCollections.parallelStream().filter { pathCollection ->
                    pathCollection.includePattern.any { PathUtils.toPath(requestPath!!).startsWith(it) }
                }.collect(Collectors.toList())
                // 只要有个路径没有配置权限则返回没权限
                if (collections.isEmpty()) return false
                requestPath to collections
            }.toMap()

            val pathCollectionIds = pathToPathCollections.flatMap { it.value }.map { it.id!! }

            val repoPathCollectPermission = devOpsAuthGeneral.getRepoPathCollectPermission(
                uid,
                projectId!!,
                listOf(repoName!!),
                // 兼容以后任意权限的情况，同时也避免pathCollectionIds为空导致查出全部
                pathCollectionIds.plus("*")
            )
            val authCollectionIds =
                repoPathCollectPermission.filter { it.actionCode == action.id() }
                    .map { it.instanceId }

            val filter = pathToPathCollections.filter { (requestPath, pathMatchCollections) ->
                // 只要有一个路径有权限，则返回有权限
                val pathMatchCollectionIds = pathMatchCollections.map { it.id!! }
                // 存在权限
                authCollectionIds.intersect(pathMatchCollectionIds).isNotEmpty()
            }

            // 不为空则所以有权限
            return filter.isNotEmpty()

        }
    }

    override fun createPermission(request: CreatePermissionRequest): Boolean {
        logger.info("create  permission request : [$request]")
        val permission = permissionDao.findOneByPermName(
            permName = request.permName,
            projectId = request.projectId!!,
            resourceType = request.resourceType,
            repoName = request.repos.first()
        )
        permission?.let {
            logger.warn("create permission  [$request] is exist.")
            throw ErrorCodeException(AuthMessageCode.AUTH_DUP_PERMNAME)
        }
        val result = permissionDao.insert(
            TPermission(
                resourceType = request.resourceType,
                projectId = request.projectId,
                permName = request.permName,
                repos = request.repos,
                includePattern = request.includePattern,
                excludePattern = request.excludePattern,
                users = request.users,
                roles = request.roles,
                createBy = request.createBy,
                createAt = LocalDateTime.now(),
                updatedBy = request.updatedBy,
                updateAt = LocalDateTime.now()
            )
        )
        result.id?.let {
            return true
        }
        return false
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

    override fun getUserAuthPaths(
        userId: String,
        projectId: String,
        repoNames: List<String>,
        action: PermissionAction
    ): Map<String, List<String>> {

        if (devOpsAuthGeneral.isProjectOrSuperiorAdmin(userId, projectId)) {
            return repoNames.associateWith { listOf("/") }
        }

        val repoAuthPathMap = repoNames.associateWith { mutableListOf<String>() }.toMutableMap()

        repoNames.forEach { repoName ->
            if (devOpsAuthGeneral.validateUserPermission(
                    projectId = projectId,
                    option = UserPermissionValidateDTO(
                        userId = userId,
                        instanceId = repoName,
                        resourceCode = RESOURCECODE,
                        actionCodes = listOf(action.id())
                    )
                )
            ) {
                repoAuthPathMap[repoName] = mutableListOf("/")
            }
        }


        val repoPathPermissions = devOpsAuthGeneral.getRepoPathCollectPermission(
            userId,
            projectId,
            repoNames,
            // 为表示查出全部
            emptyList()
        )

        val repoPathCollectionsIds =
            repoPathPermissions.filter { it.actionCode == action.id() }
                .map { it.instanceId }
        val authPathCollections = permissionDao.findByIdIn(repoPathCollectionsIds)
        authPathCollections.groupBy { it.repos.first() }.map { repoGroup ->
            repoAuthPathMap[repoGroup.key]?.addAll(repoGroup.value.flatMap { it.includePattern }.distinct())
        }
        return repoAuthPathMap.filter { it.value.isNotEmpty() }
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
        return listOf(repoAdmin, repoUser).map { PermRequestUtil.convToPermission(it) }
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
        val user = userDao.findFirstByUserId(userId) ?: run {
            return listOf()
        }

        // 用户为CI 管理员
        if (isCIAdmin(userId)) {
            return projectClient.listProject().map { it.name }
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
        actions: List<PermissionAction>?,
        includePathAuthRepo: Boolean
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
            if (includePathAuthRepo) {
                // 获取用户有路径权限的仓库集合
                repoList.addAll(devOpsAuthGeneral.getRepoWithPathPermission(projectId, userId))
            }
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
        } else if (projectId != null && tenantId != null) {
            logger.info("check user $userId is CI project admin of [project: $projectId]")
            devOpsAuthGeneral.isProjectOrSuperiorAdmin(userId, projectId)
        } else {
            return false
        }
    }

    companion object {
        val logger: Logger = LoggerFactory.getLogger(CanwayPermissionServiceImpl::class.java)
    }
}
