package com.tencent.bkrepo.auth.service.impl

import com.tencent.bkrepo.auth.api.CanwayCustomMigrationClient
import com.tencent.bkrepo.auth.api.CanwayCustomPermissionClient
import com.tencent.bkrepo.auth.api.CanwayCustomRoleClient
import com.tencent.bkrepo.auth.api.CanwayProjectClient
import com.tencent.bkrepo.auth.api.ServicePermissionResource
import com.tencent.bkrepo.auth.constant.AUTH_BUILTIN_VIEWER
import com.tencent.bkrepo.auth.constant.AUTH_BUILTIN_ADMIN
import com.tencent.bkrepo.auth.constant.AUTH_BUILTIN_USER
import com.tencent.bkrepo.auth.constant.AUTH_ADMIN
import com.tencent.bkrepo.auth.constant.AuthConstant.ANY_RESOURCE_CODE
import com.tencent.bkrepo.auth.constant.PROJECT_VIEW_PERMISSION
import com.tencent.bkrepo.auth.constant.AuthConstant.CPACK_MANAGER
import com.tencent.bkrepo.auth.constant.AuthConstant.CPACK_USER
import com.tencent.bkrepo.auth.constant.AuthConstant.CPACK_VIEWERS
import com.tencent.bkrepo.auth.constant.AuthConstant.SCOPECODE
import com.tencent.bkrepo.auth.constant.AuthConstant.SUBJECTCODE
import com.tencent.bkrepo.auth.constant.AuthRoleType.PROJECT_ROLE
import com.tencent.bkrepo.auth.constant.AuthSubjectCode.GROUP
import com.tencent.bkrepo.auth.constant.AuthSubjectCode.USER
import com.tencent.bkrepo.auth.general.DevOpsAuthGeneral
import com.tencent.bkrepo.auth.message.AuthMessageCode
import com.tencent.bkrepo.auth.model.TPermission
import com.tencent.bkrepo.auth.pojo.CanwayBkrepoPathPermission
import com.tencent.bkrepo.auth.pojo.CanwayBkrepoPermission
import com.tencent.bkrepo.auth.pojo.enums.PermissionAction
import com.tencent.bkrepo.auth.pojo.enums.ResourceType
import com.tencent.bkrepo.auth.pojo.general.ScopeDTO
import com.tencent.bkrepo.auth.pojo.migration.ActionDeleteDTO
import com.tencent.bkrepo.auth.pojo.permission.AnyResourcePermissionSaveDTO
import com.tencent.bkrepo.auth.pojo.permission.CheckPermissionRequest
import com.tencent.bkrepo.auth.pojo.permission.CreatePermissionRequest
import com.tencent.bkrepo.auth.pojo.permission.CreateRepoPathResourceTypeRequest
import com.tencent.bkrepo.auth.pojo.permission.CustomPermissionQueryDTO
import com.tencent.bkrepo.auth.pojo.permission.DeleteRepoPathResourceTypeRequest
import com.tencent.bkrepo.auth.pojo.permission.ListRepoPathInstanceRequest
import com.tencent.bkrepo.auth.pojo.permission.ListRepoPathResourceTypeRequest
import com.tencent.bkrepo.auth.pojo.permission.Permission
import com.tencent.bkrepo.auth.pojo.permission.PermissionInstanceSaveDTO
import com.tencent.bkrepo.auth.pojo.permission.PermissionVO
import com.tencent.bkrepo.auth.pojo.permission.ProjectPermissionAndAdminVO
import com.tencent.bkrepo.auth.pojo.permission.RepoPathResourceTypeInstance
import com.tencent.bkrepo.auth.pojo.permission.RepoPathResourceTypeInstance.RepoPathItem
import com.tencent.bkrepo.auth.pojo.permission.ResourcePermissionSaveDTO
import com.tencent.bkrepo.auth.pojo.permission.SaveRepoPathPermission
import com.tencent.bkrepo.auth.pojo.permission.UpdatePermissionPathRequest
import com.tencent.bkrepo.auth.pojo.permission.UpdateRepoPathResourceTypeRequest
import com.tencent.bkrepo.auth.pojo.permission.UserPermissionQueryDTO
import com.tencent.bkrepo.auth.pojo.role.RoleCreateDTO
import com.tencent.bkrepo.auth.pojo.role.RoleVO
import com.tencent.bkrepo.auth.pojo.role.SubjectDTO
import com.tencent.bkrepo.auth.service.PermissionService
import com.tencent.bkrepo.common.api.exception.ErrorCodeException
import com.tencent.bkrepo.common.api.exception.ParameterInvalidException
import com.tencent.bkrepo.common.api.pojo.Response
import com.tencent.bkrepo.common.artifact.path.PathUtils
import com.tencent.bkrepo.common.artifact.pojo.RepositoryType
import com.tencent.bkrepo.common.devops.CATELOG_RESOURCECODE
import com.tencent.bkrepo.common.devops.REPLICA_RESOURCECODE
import com.tencent.bkrepo.common.devops.REPO_PATH_RESOURCECODE
import com.tencent.bkrepo.common.devops.REPO_PATH_SCOPE_CODE
import com.tencent.bkrepo.common.devops.RESOURCECODE
import com.tencent.bkrepo.common.devops.SEARCH_RESOURCECODE
import com.tencent.bkrepo.common.security.exception.PermissionException
import com.tencent.bkrepo.common.service.util.ResponseBuilder
import com.tencent.bkrepo.repository.api.ProjectClient
import com.tencent.bkrepo.repository.api.RepositoryClient
import io.swagger.annotations.ApiParam
import net.canway.devops.auth.api.custom.CanwayCustomResourceTypeClient
import net.canway.devops.auth.pojo.resource.action.ResourceActionVO
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.bind.annotation.RequestAttribute
import org.springframework.web.bind.annotation.RequestParam

@Service
class ExtPermissionServiceImpl(
    private val permissionService: PermissionService,
    private val projectClient: ProjectClient,
    private val repositoryClient: RepositoryClient,
    private val canwayCustomPermissionClient: CanwayCustomPermissionClient,
    private val canwayCustomRoleClient: CanwayCustomRoleClient,
    private val canwayProjectClient: CanwayProjectClient,
    private val servicePermissionResource: ServicePermissionResource,
    private val canwayCustomResourceTypeClient: CanwayCustomResourceTypeClient,
    private val devOpsAuthGeneral: DevOpsAuthGeneral,
    private val canwayCustomMigrationClient: CanwayCustomMigrationClient,
) {
    @Suppress("TooGenericExceptionCaught")
    fun migHistoryPermissionData() {
        // 加载全部项目
        val projectList = projectClient.listProject().data ?: return
        for (project in projectList) {
            val repoList = repositoryClient.listRepo(project.name).data ?: return
            for (repo in repoList) {
                // 加载原仓库内置权限
                val builtinPermissions = permissionService.listPermission(project.name, repo.name)
                // 历史数据中存在历史数据，先对历史数据去重
                val targetList = mergePermission(builtinPermissions)
                for (permission in targetList) {
                    // 迁移权限
                    migrateActions(permission)
                    // 迁移用户,用户组,部门
                    migrateUsers(permission)
                }
                // 迁移完成后移除原仓库级的访问者permission
                if (targetList.map { it.permName }.contains(AUTH_BUILTIN_VIEWER)) {
                    permissionService.deletePermission(
                        targetList.first {
                            it.permName == AUTH_BUILTIN_VIEWER
                        }.id!!
                    )
                }
            }
        }
    }

    @Suppress("TooGenericExceptionCaught")
    fun migrateToDevOps() {
        // 获取所有项目
        val projectList = projectClient.listProject().data ?: listOf()
        // 每个项目创建角色（查看者、使用者与仓库管理者）
        projectList.forEach { project ->
            try {
                logger.info("${project.name} project start creating role")
                val cpackViewer = roleCreate(project.name, CPACK_VIEWERS)
                val cpackUser = roleCreate(project.name, CPACK_USER)
                val cpackManager = roleCreate(project.name, CPACK_MANAGER)

                // 查询项目所有成员
                val projectMember =
                    canwayProjectClient.listMember(projectId = project.name).data?.map { it.userId } ?: listOf()

                logger.info("${project.name} project starting to obtain product library permissions user")
                // 创建角色用户与用户组
                val projectRepoUserIdList = mutableListOf<SubjectDTO>()
                val projectRepoUserRoleList = mutableListOf<SubjectDTO>()

                val projectRepoManagerIdList = mutableListOf<SubjectDTO>()
                val projectRepoManagerRoleList = mutableListOf<SubjectDTO>()

                // 查询项目下所有仓库的内置权限
                val allRepo = repositoryClient.listRepo(projectId = project.name).data?.map { it.name } ?: listOf()
                allRepo.forEach { repoName ->
                    val repoPermission = servicePermissionResource.listRepoBuiltinPermission(
                        projectId = project.name,
                        repoName = repoName
                    ).data ?: listOf()
                    // 仓库管理员加入用户与用户组
                    projectRepoManagerIdList.addAll(
                        repoPermission.filter { it.permName == AUTH_BUILTIN_ADMIN }
                            .flatMap { it.users }.map { SubjectDTO(subjectCode = USER, subjectId = it) }
                    )
                    projectRepoManagerRoleList.addAll(
                        repoPermission.filter { it.permName == AUTH_BUILTIN_ADMIN }
                            .flatMap { it.roles }.map { SubjectDTO(subjectCode = GROUP, subjectId = it) }
                    )

                    // 仓库使用者加入用户与用户组
                    projectRepoUserIdList.addAll(
                        repoPermission.filter { it.permName == AUTH_BUILTIN_USER }
                            .flatMap { it.users }.map { SubjectDTO(subjectCode = USER, subjectId = it) }
                    )
                    projectRepoUserRoleList.addAll(
                        repoPermission.filter { it.permName == AUTH_BUILTIN_USER }
                            .flatMap { it.roles }.map { SubjectDTO(subjectCode = GROUP, subjectId = it) }
                    )
                }

                logger.info("${project.name} project start migrating users")
                // 查看者加入用户与用户组
                canwayCustomRoleClient.batchAddRoleMember(
                    userId = AUTH_ADMIN,
                    scopeCode = SCOPECODE,
                    scopeId = project.name,
                    id = cpackViewer.id,
                    subjects = projectMember.map { SubjectDTO(subjectCode = USER, subjectId = it) }.distinct()
                )

                // 使用者加入用户与用户组
                canwayCustomRoleClient.batchAddRoleMember(
                    userId = AUTH_ADMIN,
                    scopeCode = SCOPECODE,
                    scopeId = project.name,
                    id = cpackUser.id,
                    subjects = projectRepoUserIdList.distinct()
                )
                canwayCustomRoleClient.batchAddRoleMember(
                    userId = AUTH_ADMIN,
                    scopeCode = SCOPECODE,
                    scopeId = project.name,
                    id = cpackUser.id,
                    subjects = projectRepoUserRoleList.distinct()
                )

                // 管理者加入用户与用户组
                canwayCustomRoleClient.batchAddRoleMember(
                    userId = AUTH_ADMIN,
                    scopeCode = SCOPECODE,
                    scopeId = project.name,
                    id = cpackManager.id,
                    subjects = projectRepoManagerIdList.distinct()
                )
                canwayCustomRoleClient.batchAddRoleMember(
                    userId = AUTH_ADMIN,
                    scopeCode = SCOPECODE,
                    scopeId = project.name,
                    id = cpackManager.id,
                    subjects = projectRepoManagerRoleList.distinct()
                )
                logger.info("${project.name} project migration completed")
            } catch (exception: Exception) {
                logger.error("${project.name} project permission merge fail:${exception.message}")
            }
        }
    }

    fun listDevOpsPermission(userId: String, projectId: String, repoName: String?): List<CanwayBkrepoPermission> {
        val canwayBkrepoPermission = mutableListOf<CanwayBkrepoPermission>()
        // 制品仓库实例数据权限动作
        repoName?.let {
            val repoInstanceAction = getCanwayPermission(userId, projectId, it, RESOURCECODE).permissions
            canwayBkrepoPermission.add(
                CanwayBkrepoPermission(
                    resourceCode = RESOURCECODE,
                    actionCodes = if (repoInstanceAction.isEmpty()) {
                        emptyList()
                    } else {
                        repoInstanceAction.flatMap { it.actionCodes }.distinct()
                    }
                )
            )
            return canwayBkrepoPermission
        }
        // 所有动作权限（非实例）
        listOf(RESOURCECODE, CATELOG_RESOURCECODE, SEARCH_RESOURCECODE, REPLICA_RESOURCECODE).forEach { resourceCode ->
            val repoActionList = getCanwayPermission(userId, projectId, repoName, resourceCode).permissions
            canwayBkrepoPermission.add(
                CanwayBkrepoPermission(
                    resourceCode = resourceCode,
                    actionCodes = if (repoActionList.isEmpty()) {
                        emptyList()
                    } else {
                        repoActionList.filter {
                            it.instanceId == ANY_RESOURCE_CODE
                        }[0].actionCodes
                    }
                )
            )
        }
        return canwayBkrepoPermission
    }


    /**
     * 复用repo-permission表，路径资源实例存储在。但实际权限在devopos-auth服务中
     */
    fun createRepoPathCollectionResourceType(
        userId: String,
        request: CreateRepoPathResourceTypeRequest
    ): Response<Boolean> {

        checkRepoPathPermissionParam(userId, request.projectId, request.repos, request.permName, request.includePattern)
        logger.info("userId $userId createRepoPathResourceType")
        return ResponseBuilder.success(
            permissionService.createPermission(
                CreatePermissionRequest(
                    resourceType = ResourceType.NODE,
                    projectId = request.projectId,
                    permName = request.permName,
                    repos = request.repos,
                    includePattern = request.includePattern.map { PathUtils.normalizePath(it) },
                    actions = listOf(),
                    createBy = userId,
                    updatedBy = userId
                )
            )
        )
    }

    @Transactional(rollbackFor = [Throwable::class])
    fun updateRepoPathCollectionResourceType(userId: String, request: UpdateRepoPathResourceTypeRequest) {
        val permission = permissionService.findPermissionById(request.permissionId)
            ?: throw ErrorCodeException(AuthMessageCode.AUTH_PERMISSION_NOT_EXIST)
        checkRepoPathPermissionParam(
            userId,
            request.projectId,
            permission.repos,
            request.permName,
            request.includePattern
        )
        logger.info("userId $userId updateRepoPathResourceType")

        permissionService.updatePermissionById(
            request.permissionId,
            "permName",
            request.permName
        )
        permissionService.updateIncludePath(
            UpdatePermissionPathRequest(
                permissionId = request.permissionId,
                request.includePattern.map { PathUtils.normalizePath(it) })
        )
    }

    fun deleteRepoPathCollectionResourceType(userId: String, request: DeleteRepoPathResourceTypeRequest) {
        val permission = permissionService.findPermissionById(request.permissionId)
            ?: throw ErrorCodeException(AuthMessageCode.AUTH_PERMISSION_NOT_EXIST)
        hasSetRepoPermission(request.projectId, permission.repos.first(), userId)
        logger.info("userId $userId deleteRepoPathResourceType")
        permissionService.deletePermission(request.permissionId)
    }

    fun listRepoPathCollectionResourceType(userId: String, request: ListRepoPathResourceTypeRequest): List<Permission> {
        return permissionService.listNodePermission(request.projectId, request.repo)
    }


    fun listRepoPathCollectionResourceInstance(
        userId: String,
        request: ListRepoPathInstanceRequest
    ): List<RepoPathResourceTypeInstance> {
        val permission = permissionService.listNodePermission(request.projectId, request.repo)
        val repoMap = repositoryClient.listRepo(request.projectId).data?.associateBy { it.name } ?: emptyMap()
        return permission.groupBy { it.repos.first() }.map { group ->
            RepoPathResourceTypeInstance(
                group.key,
                repoMap[group.key]?.type?.name?.toLowerCase() ?: "",
                group.value.map { RepoPathItem(it.id!!, it.permName) }
            )
        }.filter {
            // 路径授权只考虑二进制仓库
            it.repoType == RepositoryType.GENERIC.name.toLowerCase()
        }
    }

    private fun checkRepoPathPermissionParam(
        userId: String,
        projectId: String,
        repos: List<String>,
        permName: String,
        includePattern: List<String>
    ) {
        if (repos.isEmpty()) {
            throw ParameterInvalidException("repos is empty")
        }

        if (includePattern.isEmpty()) {
            throw ParameterInvalidException("includePattern is empty")
        }

        if (!hasSetRepoPermission(projectId, repos.first(), userId)) {
            throw PermissionException()
        }

        //校验路径集合名称，支持中文、英文、数字、中划线、下划线，限制 32 个字符
        val pattern = "^[a-zA-Z0-9_\u4e00-\u9fa5-]{1,32}$"

        if (!permName.matches(pattern.toRegex())) {
            throw ParameterInvalidException("permName param Invalid")
        }
    }

    private fun hasSetRepoPermission(projectId: String, repoName: String, userId: String): Boolean {
        return permissionService.checkPermission(
            CheckPermissionRequest(
                uid = userId,
                projectId = projectId,
                resourceType = ResourceType.REPO,
                action = PermissionAction.MANAGE,
                repoName = repoName
            )
        )
    }

    /**
     * [originPerm]  要保留的权限
     * [permission]  要删除的权限
     */
    private fun mergePermissionData(originPerm: Permission, permission: Permission, isRepeat: Boolean) {
        // 合并用户
        val targetUsers = originPerm.users.toMutableSet().apply {
            addAll(permission.users.toSet())
        }
        permissionService.updatePermissionById(
            id = originPerm.id!!,
            key = TPermission::users.name,
            value = targetUsers
        )

        // 合并用户组
        val targetRoles = originPerm.roles.toMutableSet().apply {
            addAll(permission.roles)
        }
        permissionService.updatePermissionById(
            id = originPerm.id!!,
            key = TPermission::roles.name,
            value = targetRoles
        )

        // 合并部门
        val targetDepartments = originPerm.departments.toMutableSet().apply {
            addAll(permission.departments)
        }
        permissionService.updatePermissionById(
            id = originPerm.id!!,
            key = TPermission::departments.name,
            value = targetDepartments
        )

        // 如果权限名 AUTH_BUILTIN_USER 则需要合并action
        if (isRepeat && originPerm.permName == AUTH_BUILTIN_USER) {
            val targetActions = originPerm.actions.toMutableSet().apply {
                addAll(permission.actions)
            }
            permissionService.updatePermissionById(
                id = originPerm.id!!,
                key = TPermission::actions.name,
                value = targetActions
            )
        }
    }

    /**
     * permission 去重，去重之前合并数据
     */
    private fun mergePermission(permissionList: List<Permission>): List<Permission> {
        val list = mutableListOf<Permission>()
        val repeatList = mutableListOf<Permission>()
        for (permission in permissionList) {
            if (list.map { it.permName }.contains(permission.permName)) {
                val originPerm = list.first { it.permName == permission.permName }
                mergePermissionData(originPerm, permission, isRepeat = true)
                repeatList.add(permission)
            } else {
                list.add(permission)
            }
        }

        for (repeatPerm in repeatList) {
            permissionService.deletePermission(repeatPerm.id!!)
        }
        return list
    }

    /**
     * 将原仓库级permission 关联的用户全部添加到项目级permission
     * 且删除不符合条件的permission
     */
    private fun migrateUsers(permission: Permission) {
        val projectId = permission.projectId ?: return
        when (permission.permName) {
            AUTH_BUILTIN_VIEWER, AUTH_BUILTIN_USER, AUTH_BUILTIN_ADMIN -> {
                // 用户移动至项目用户
                val projectView = permissionService.listProjectBuiltinPermission(projectId)
                    .first { it.permName == PROJECT_VIEW_PERMISSION }
                mergePermissionData(projectView, permission, isRepeat = false)
            }

            else -> permissionService.deletePermission(permission.id!!)
        }
    }

    private fun migrateActions(permission: Permission) {
        val actions = permission.actions
        val resultActions = when (permission.permName) {
            AUTH_BUILTIN_ADMIN -> mutableSetOf(PermissionAction.MANAGE)
            AUTH_BUILTIN_USER -> {
                val set = mutableSetOf<PermissionAction>()
                for (action in actions) {
                    set.addAll(transAction(action))
                }
                set
            }

            else -> return
        }
        try {
            permissionService.updatePermissionById(
                id = permission.id!!,
                key = TPermission::actions.name,
                value = resultActions.toList()
            )
        } catch (e: Exception) {
            logger.error("migrate: [${permission.id}] failed")
            logger.error("$e")
        }
    }

    private fun transAction(action: PermissionAction): Set<PermissionAction> {
        // PermissionAction.READ 全部落到项目级
        return when (action) {
            PermissionAction.REPO_MANAGE -> setOf(PermissionAction.MANAGE)
            PermissionAction.FOLDER_MANAGE -> setOf(PermissionAction.WRITE, PermissionAction.DELETE)
            PermissionAction.ARTIFACT_COPY -> setOf(PermissionAction.WRITE)
            PermissionAction.ARTIFACT_RENAME -> setOf(PermissionAction.UPDATE)
            PermissionAction.ARTIFACT_MOVE -> setOf(PermissionAction.WRITE)
            PermissionAction.ARTIFACT_SHARE -> setOf()
//            PermissionAction.ARTIFACT_DOWNLOAD -> setOf(PermissionAction.READ)
//            PermissionAction.ARTIFACT_READWRITE -> setOf(PermissionAction.WRITE, PermissionAction.READ)
            PermissionAction.ARTIFACT_READWRITE -> setOf(PermissionAction.WRITE)
//            PermissionAction.ARTIFACT_READ -> setOf(PermissionAction.READ)
            PermissionAction.ARTIFACT_UPDATE -> setOf(PermissionAction.UPDATE)
            PermissionAction.ARTIFACT_DELETE -> setOf(PermissionAction.DELETE)
            else -> setOf()
        }
    }

    private fun roleCreate(projectId: String, roleType: String): RoleVO {
        // 创建权限中心角色
        val cpackRole = canwayCustomRoleClient.createRole(
            userId = AUTH_ADMIN,
            scopeCode = SCOPECODE,
            scopeId = projectId,
            role = RoleCreateDTO(
                name = roleType,
                desc = roleType,
                type = PROJECT_ROLE
            )
        ).data ?: throw ErrorCodeException(AuthMessageCode.AUTH_DUP_APPID)
        // 权限列表
        val permissionList = mutableListOf<AnyResourcePermissionSaveDTO>()
        permissionList.addAll(
            listOf(
                AnyResourcePermissionSaveDTO(
                    resourceCode = RESOURCECODE,
                    actionCode = PermissionAction.ACCESS.name.toLowerCase()
                ),
                AnyResourcePermissionSaveDTO(
                    resourceCode = RESOURCECODE,
                    actionCode = PermissionAction.READ.name.toLowerCase()
                ),
                AnyResourcePermissionSaveDTO(
                    resourceCode = CATELOG_RESOURCECODE,
                    actionCode = PermissionAction.VIEW.name.toLowerCase()
                ),
                AnyResourcePermissionSaveDTO(
                    resourceCode = SEARCH_RESOURCECODE,
                    actionCode = PermissionAction.VIEW.name.toLowerCase()
                ),
            )
        )
        when (roleType) {
            CPACK_USER -> {
                permissionList.addAll(
                    listOf(
                        AnyResourcePermissionSaveDTO(
                            resourceCode = RESOURCECODE,
                            actionCode = PermissionAction.WRITE.name.toLowerCase()
                        ),
                        AnyResourcePermissionSaveDTO(
                            resourceCode = RESOURCECODE,
                            actionCode = PermissionAction.UPDATE.name.toLowerCase()
                        ),
                        AnyResourcePermissionSaveDTO(
                            resourceCode = RESOURCECODE,
                            actionCode = PermissionAction.SHARE.name.toLowerCase()
                        ),
                        AnyResourcePermissionSaveDTO(
                            resourceCode = RESOURCECODE,
                            actionCode = PermissionAction.FORBID.name.toLowerCase()
                        ),
                        AnyResourcePermissionSaveDTO(
                            resourceCode = RESOURCECODE,
                            actionCode = PermissionAction.LOCK.name.toLowerCase()
                        )
                    )
                )
            }

            CPACK_MANAGER -> {
                permissionList.addAll(
                    listOf(
                        AnyResourcePermissionSaveDTO(
                            resourceCode = RESOURCECODE,
                            actionCode = PermissionAction.WRITE.name.toLowerCase()
                        ),
                        AnyResourcePermissionSaveDTO(
                            resourceCode = RESOURCECODE,
                            actionCode = PermissionAction.UPDATE.name.toLowerCase()
                        ),
                        AnyResourcePermissionSaveDTO(
                            resourceCode = RESOURCECODE,
                            actionCode = PermissionAction.CREATE.name.toLowerCase()
                        ),
                        AnyResourcePermissionSaveDTO(
                            resourceCode = RESOURCECODE,
                            actionCode = PermissionAction.DELETE.name.toLowerCase()
                        ),
                        AnyResourcePermissionSaveDTO(
                            resourceCode = RESOURCECODE,
                            actionCode = PermissionAction.MANAGE.name.toLowerCase()
                        ),
                        AnyResourcePermissionSaveDTO(
                            resourceCode = RESOURCECODE,
                            actionCode = PermissionAction.REPO_DELETE.name.toLowerCase()
                        ),
                        AnyResourcePermissionSaveDTO(
                            resourceCode = RESOURCECODE,
                            actionCode = PermissionAction.SHARE.name.toLowerCase()
                        ),
                        AnyResourcePermissionSaveDTO(
                            resourceCode = RESOURCECODE,
                            actionCode = PermissionAction.FORBID.name.toLowerCase()
                        ),
                        AnyResourcePermissionSaveDTO(
                            resourceCode = RESOURCECODE,
                            actionCode = PermissionAction.LOCK.name.toLowerCase()
                        )
                    )
                )
            }
        }
        // 查看者授权
        canwayCustomPermissionClient.saveAnyPermissions(
            userId = AUTH_ADMIN,
            scopeCode = SCOPECODE,
            scopeId = projectId,
            subjectCode = SUBJECTCODE,
            subjectId = cpackRole.id,
            resourceLevel = SCOPECODE,
            permissions = permissionList
        )
        return cpackRole
    }

    private fun getCanwayPermission(
        userId: String,
        projectId: String,
        repoName: String?,
        resourceCode: String,
        actions: List<String> = emptyList()
    ): ProjectPermissionAndAdminVO {
        return canwayProjectClient.getUserPermission(
            projectId = projectId,
            UserPermissionQueryDTO(
                userId = userId,
                resourceCode = resourceCode,
                actionCodes = actions,
                instanceIds = if (repoName.isNullOrBlank()) listOf(ANY_RESOURCE_CODE) else listOf(repoName),
                paddingInstancePermission = false
            )
        ).data!!
    }

    fun listRepoPathCollectionResourceAction(userId: String): List<ResourceActionVO> {
        return canwayCustomResourceTypeClient.listResourceAction(userId, listOf(REPO_PATH_RESOURCECODE)).data
            ?: emptyList()
    }

    fun listRepoPathCollectionPermissions(
        projectId: String,
        subjectCode: String,
        subjectId: String
    ): List<PermissionVO> {
        val permission = permissionService.listNodePermission(projectId, null)
        return canwayCustomPermissionClient.queryPermission(
            CustomPermissionQueryDTO(
                scopes = permission.flatMap { it.repos }
                    .map {
                        ScopeDTO(
                            scopeCode = REPO_PATH_SCOPE_CODE,
                            scopeId = getCanwayRepoPathScopeId(projectId, it)
                        )
                    },
                subjects = listOf(
                    SubjectDTO(
                        subjectCode = subjectCode,
                        subjectId = subjectId
                    )
                ),
                resourceCodes = listOf(REPO_PATH_RESOURCECODE),
                instanceIds = permission.map { it.id!! }
            )
        ).data ?: emptyList()
    }

    fun saveRepoPathCollectionPermissions(
        userId: String,
        projectId: String,
        subjectCode: String,
        subjectId: String,
        request: SaveRepoPathPermission
    ) {
        logger.info("user $userId save repo path permission")
        request.repoPathPermission.forEach {
            canwayCustomPermissionClient.saveResourcePermissions(
                userId = userId,
                scopeCode = REPO_PATH_SCOPE_CODE,
                scopeId = getCanwayRepoPathScopeId(projectId, it.repoName),
                subjectCode = subjectCode,
                subjectId = subjectId,
                resources = ResourcePermissionSaveDTO(
                    resourceCode = REPO_PATH_RESOURCECODE,
                    anyInstancePermission = emptyList(),
                    instancePermission = it.instancePermission.map {
                        PermissionInstanceSaveDTO(
                            instanceId = it.instanceId,
                            actionCode = it.actionCode
                        )
                    }
                )
            )
        }
    }

    fun listUserRepoPathCollectionFinalPermissions(userId: String, projectId: String): List<PermissionVO> {
        val subjects =
            canwayProjectClient.getUserRelatedRoleAndPermissionScope(userId, projectId).data?.map { it.first }
                ?: listOf(SubjectDTO.user(userId))
        val repoPathResourceType = permissionService.listNodePermission(projectId, null)
        val scopes = repoPathResourceType.flatMap { it.repos }
            .map { ScopeDTO(scopeCode = REPO_PATH_SCOPE_CODE, scopeId = getCanwayRepoPathScopeId(projectId, it)) }
        return canwayCustomPermissionClient.queryPermission(
            CustomPermissionQueryDTO(
                scopes = scopes,
                subjects = subjects,
                resourceCodes = listOf(REPO_PATH_RESOURCECODE),
            )
        ).data ?: emptyList()
    }

    fun listRepoPermissionAction(
        userId: String,
        projectId: String,
        repoName: String,
        path: String
    ): CanwayBkrepoPathPermission {
        val projectOrSuperiorAdmin = devOpsAuthGeneral.isProjectOrSuperiorAdmin(userId, projectId)
        if (projectOrSuperiorAdmin) {
            // 管理员直接返回全部action
            val resourceActions =
                canwayCustomResourceTypeClient.listResourceAction(userId, listOf(REPO_PATH_RESOURCECODE)).data
                    ?: emptyList()
            return CanwayBkrepoPathPermission(REPO_PATH_RESOURCECODE, resourceActions.map { it.actionCode })
        }

        // 仓库权限动作
        val repoInstanceAction = getCanwayPermission(userId, projectId, repoName, RESOURCECODE).permissions
        val repoActions = repoInstanceAction.flatMap { it.actionCodes }

        val pathCollection = permissionService.listNodePermission(projectId, repoName)
        // 获取包含请求路径的匹配的路径集合
        val matchPathCollection = pathCollection.filter { collection ->
            collection.includePattern.any { path.startsWith(PathUtils.toPath(it)) }
        }
        // 如果没有匹配的路径集合，则返回仓库
        if (matchPathCollection.isEmpty()) return CanwayBkrepoPathPermission(REPO_PATH_RESOURCECODE, repoActions)

        // 获取用户权限
        val repoPathCollectPermission = devOpsAuthGeneral.getRepoPathCollectPermission(
            userId,
            projectId,
            listOf(repoName),
            matchPathCollection.map { it.id!! })
        return CanwayBkrepoPathPermission(
            REPO_PATH_RESOURCECODE,
            repoPathCollectPermission.map { it.actionCode }.plus(repoActions).distinct()
        )
    }


    private fun getCanwayRepoPathScopeId(projectId: String, repoName: String): String {
        return "${projectId}_${repoName}"
    }

    companion object {
        private val logger: Logger = LoggerFactory.getLogger(ExtPermissionServiceImpl::class.java)
    }
}
