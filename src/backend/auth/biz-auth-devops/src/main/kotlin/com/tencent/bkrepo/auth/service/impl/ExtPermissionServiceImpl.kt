package com.tencent.bkrepo.auth.service.impl

import com.tencent.bkrepo.auth.constant.AUTH_BUILTIN_ADMIN
import com.tencent.bkrepo.auth.constant.AUTH_BUILTIN_USER
import com.tencent.bkrepo.auth.constant.AUTH_BUILTIN_VIEWER
import com.tencent.bkrepo.auth.constant.PROJECT_VIEW_PERMISSION
import com.tencent.bkrepo.auth.model.TPermission
import com.tencent.bkrepo.auth.pojo.enums.PermissionAction
import com.tencent.bkrepo.auth.pojo.permission.Permission
import com.tencent.bkrepo.auth.service.PermissionService
import com.tencent.bkrepo.repository.api.ProjectClient
import com.tencent.bkrepo.repository.api.RepositoryClient
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class ExtPermissionServiceImpl(
    private val permissionService: PermissionService,
    private val projectClient: ProjectClient,
    private val repositoryClient: RepositoryClient
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
                for (permission in builtinPermissions) {
                    // 迁移权限
                    migrateActions(permission)
                    // 迁移用户,用户组,部门
                    migrateUsers(permission)
                }
                // 迁移完成后移除原仓库级的访问者permission
                if (builtinPermissions.map { it.permName }.contains(AUTH_BUILTIN_VIEWER)) {
                    permissionService.deletePermission(
                        builtinPermissions.first {
                            it.permName == AUTH_BUILTIN_VIEWER
                        }.id!!
                    )
                }
            }
        }
    }

    private fun migrateUsers(permission: Permission) {
        val projectId = permission.projectId ?: return
        when (permission.permName) {
            AUTH_BUILTIN_VIEWER, AUTH_BUILTIN_USER, AUTH_BUILTIN_ADMIN -> {
                // 用户移动至项目用户
                val projectView = permissionService.listProjectBuiltinPermission(projectId)
                    .first { it.permName == PROJECT_VIEW_PERMISSION }
                val targetUsers = projectView.users.toMutableSet().apply {
                    addAll(permission.users.toSet())
                }
                permissionService.updatePermissionById(
                    id = projectView.id!!,
                    key = TPermission::users.name,
                    value = targetUsers
                )

                val targetRoles = projectView.roles.toMutableSet().apply {
                    addAll(permission.roles)
                }
                permissionService.updatePermissionById(
                    id = projectView.id!!,
                    key = TPermission::roles.name,
                    value = targetRoles
                )

                val targetDepartments = projectView.departments.toMutableSet().apply {
                    addAll(permission.departments)
                }
                permissionService.updatePermissionById(
                    id = projectView.id!!,
                    key = TPermission::departments.name,
                    value = targetDepartments
                )
            }
            else -> permissionService.deletePermission(permission.id!!)
        }
    }

    private fun migrateActions(permission: Permission) {
        val actions = permission.actions
        val resultActions = mutableSetOf<PermissionAction>()
        for (action in actions) {
            resultActions.addAll(transAction(action))
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
        return when (action) {
            PermissionAction.REPO_MANAGE -> setOf(PermissionAction.MANAGE)
            PermissionAction.FOLDER_MANAGE -> setOf(PermissionAction.WRITE, PermissionAction.DELETE)
            PermissionAction.ARTIFACT_COPY -> setOf(PermissionAction.WRITE)
            PermissionAction.ARTIFACT_RENAME -> setOf(PermissionAction.UPDATE)
            PermissionAction.ARTIFACT_MOVE -> setOf(PermissionAction.WRITE)
            PermissionAction.ARTIFACT_SHARE -> setOf()
            PermissionAction.ARTIFACT_DOWNLOAD -> setOf(PermissionAction.READ)
            PermissionAction.ARTIFACT_READWRITE -> setOf(PermissionAction.WRITE, PermissionAction.READ)
            PermissionAction.ARTIFACT_READ -> setOf(PermissionAction.READ)
            PermissionAction.ARTIFACT_UPDATE -> setOf(PermissionAction.UPDATE)
            PermissionAction.ARTIFACT_DELETE -> setOf(PermissionAction.DELETE)
            else -> setOf()
        }
    }

    companion object {
        private val logger: Logger = LoggerFactory.getLogger(ExtPermissionServiceImpl::class.java)
    }
}
