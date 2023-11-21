package com.tencent.bkrepo.auth.service.impl

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
import com.tencent.bkrepo.auth.message.AuthMessageCode
import com.tencent.bkrepo.auth.model.TPermission
import com.tencent.bkrepo.auth.pojo.enums.PermissionAction
import com.tencent.bkrepo.auth.pojo.enums.convertEnumListToStringList
import com.tencent.bkrepo.auth.pojo.permission.AnyResourcePermissionSaveDTO
import com.tencent.bkrepo.auth.pojo.permission.Permission
import com.tencent.bkrepo.auth.pojo.permission.ProjectPermissionAndAdminVO
import com.tencent.bkrepo.auth.pojo.permission.UserPermissionQueryDTO
import com.tencent.bkrepo.auth.pojo.role.RoleCreateDTO
import com.tencent.bkrepo.auth.pojo.role.RoleVO
import com.tencent.bkrepo.auth.pojo.role.SubjectDTO
import com.tencent.bkrepo.auth.service.PermissionService
import com.tencent.bkrepo.common.api.exception.ErrorCodeException
import com.tencent.bkrepo.common.devops.CATELOG_RESOURCECODE
import com.tencent.bkrepo.common.devops.RESOURCECODE
import com.tencent.bkrepo.common.devops.SEARCH_RESOURCECODE
import com.tencent.bkrepo.repository.api.ProjectClient
import com.tencent.bkrepo.repository.api.RepositoryClient
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class ExtPermissionServiceImpl(
    private val permissionService: PermissionService,
    private val projectClient: ProjectClient,
    private val repositoryClient: RepositoryClient,
    private val canwayCustomPermissionClient: CanwayCustomPermissionClient,
    private val canwayCustomRoleClient: CanwayCustomRoleClient,
    private val canwayProjectClient: CanwayProjectClient,
    private val servicePermissionResource: ServicePermissionResource
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

    fun listDevOpsPermission(userId: String, projectId: String, repoName: String?): List<String> {
        val projectActions = convertEnumListToStringList(
            listOf(
                PermissionAction.CREATE,
                PermissionAction.REPO_DELETE,
                PermissionAction.MANAGE
            )
        )
        val repoActions = convertEnumListToStringList(
            listOf(
                PermissionAction.READ,
                PermissionAction.WRITE,
                PermissionAction.UPDATE,
                PermissionAction.SHARE,
                PermissionAction.FORBID,
                PermissionAction.LOCK,
                PermissionAction.DELETE
            )
        )
        repoName?.let {
            val noInstanceAction = getCanwayPermission(userId, projectId, it, projectActions).permissions
            return if (noInstanceAction.isEmpty()) {
                listOf()
            } else {
                noInstanceAction.filter { it.instanceId == ANY_RESOURCE_CODE }[0].actionCodes
            }
        }

        val instanceAction = getCanwayPermission(userId, projectId, repoName, repoActions).permissions
        return if (instanceAction.isEmpty()) {
            listOf()
        } else {
            instanceAction.flatMap { it.actionCodes }.distinct()
        }
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
        actions: List<String>
    ): ProjectPermissionAndAdminVO {
        return canwayProjectClient.getUserPermission(
            projectId = projectId,
            UserPermissionQueryDTO(
                userId = userId,
                resourceCode = RESOURCECODE,
                actionCodes = actions,
                instanceIds = if (repoName.isNullOrBlank()) listOf(ANY_RESOURCE_CODE) else listOf(repoName),
                paddingInstancePermission = false
            )
        ).data!!
    }

    companion object {
        private val logger: Logger = LoggerFactory.getLogger(ExtPermissionServiceImpl::class.java)
    }
}
