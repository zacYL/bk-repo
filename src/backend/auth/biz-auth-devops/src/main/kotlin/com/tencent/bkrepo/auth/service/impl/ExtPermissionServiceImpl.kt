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
                //历史数据中存在历史数据，先对历史数据去重
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

    /**
     * [originPerm]  要保留的权限
     * [permission]  要删除的权限
     */
    private fun mergePermissionData(originPerm: Permission, permission: Permission, isRepeat: Boolean) {
        //合并用户
        val targetUsers = originPerm.users.toMutableSet().apply {
            addAll(permission.users.toSet())
        }
        permissionService.updatePermissionById(
            id = originPerm.id!!,
            key = TPermission::users.name,
            value = targetUsers
        )

        //合并用户组
        val targetRoles = originPerm.roles.toMutableSet().apply {
            addAll(permission.roles)
        }
        permissionService.updatePermissionById(
            id = originPerm.id!!,
            key = TPermission::roles.name,
            value = targetRoles
        )

        //合并部门
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

    companion object {
        private val logger: Logger = LoggerFactory.getLogger(ExtPermissionServiceImpl::class.java)
    }
}
