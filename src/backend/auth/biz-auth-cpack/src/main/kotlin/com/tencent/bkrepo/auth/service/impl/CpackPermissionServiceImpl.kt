/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
 *
 * Copyright (C) 2020 THL A29 Limited, a Tencent company.  All rights reserved.
 *
 * BK-CI 蓝鲸持续集成平台 is licensed under the MIT license.
 *
 * A copy of the MIT License is included in this file.
 *
 *
 * Terms of the MIT License:
 * ---------------------------------------------------
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package com.tencent.bkrepo.auth.service.impl

import com.tencent.bkrepo.auth.constant.AUTH_BUILTIN_ADMIN
import com.tencent.bkrepo.auth.constant.AUTH_BUILTIN_USER
import com.tencent.bkrepo.auth.constant.PROJECT_MANAGE_PERMISSION
import com.tencent.bkrepo.auth.constant.PROJECT_VIEW_PERMISSION
import com.tencent.bkrepo.auth.constant.PROJECT_VIEWER_ID
import com.tencent.bkrepo.auth.constant.AUTH_ADMIN
import com.tencent.bkrepo.auth.dao.AccountDao
import com.tencent.bkrepo.auth.dao.PermissionDao
import com.tencent.bkrepo.auth.dao.PersonalPathDao
import com.tencent.bkrepo.auth.dao.RepoAuthConfigDao
import com.tencent.bkrepo.auth.dao.UserDao
import com.tencent.bkrepo.auth.dao.repository.RoleRepository
import com.tencent.bkrepo.auth.helper.PermissionHelper
import com.tencent.bkrepo.auth.message.AuthMessageCode
import com.tencent.bkrepo.auth.model.TPermission
import com.tencent.bkrepo.auth.model.TPersonalPath
import com.tencent.bkrepo.auth.pojo.enums.AccessControlMode
import com.tencent.bkrepo.auth.pojo.enums.PermissionAction
import com.tencent.bkrepo.auth.pojo.enums.ResourceType
import com.tencent.bkrepo.auth.pojo.enums.RoleType
import com.tencent.bkrepo.auth.pojo.permission.CheckPermissionRequest
import com.tencent.bkrepo.auth.pojo.permission.CreatePermissionRequest
import com.tencent.bkrepo.auth.pojo.permission.UpdatePermissionPathRequest
import com.tencent.bkrepo.auth.pojo.permission.UpdatePermissionRepoRequest
import com.tencent.bkrepo.auth.pojo.permission.CheckPermissionContext
import com.tencent.bkrepo.auth.pojo.permission.UpdatePermissionRequest
import com.tencent.bkrepo.auth.pojo.permission.UpdatePermissionRoleRequest
import com.tencent.bkrepo.auth.pojo.permission.UpdatePermissionActionRequest
import com.tencent.bkrepo.auth.pojo.permission.UpdatePermissionUserRequest
import com.tencent.bkrepo.auth.pojo.permission.UpdatePermissionDepartmentRequest
import com.tencent.bkrepo.auth.pojo.permission.Permission
import com.tencent.bkrepo.auth.pojo.permission.UpdatePermissionDeployInRepoRequest
import com.tencent.bkrepo.auth.pojo.role.ExternalRoleResult
import com.tencent.bkrepo.auth.pojo.role.RoleSource
import com.tencent.bkrepo.auth.service.PermissionService
import com.tencent.bkrepo.auth.util.request.PermRequestUtil
import com.tencent.bkrepo.common.api.constant.ANONYMOUS_USER
import com.tencent.bkrepo.common.api.exception.ErrorCodeException
import com.tencent.bkrepo.common.metadata.service.project.ProjectService
import com.tencent.bkrepo.common.metadata.service.repo.RepositoryService
import org.slf4j.LoggerFactory
import org.springframework.data.mongodb.core.query.Update
import java.time.LocalDateTime

/**
 * 产品权限实现类
 */
open class CpackPermissionServiceImpl constructor(
    private val accountDao: AccountDao,
    private val userDao: UserDao,
    private val roleRepository: RoleRepository,
    private val permissionDao: PermissionDao,
    private val personalPathDao: PersonalPathDao,
    private val repoAuthConfigDao: RepoAuthConfigDao,
    private val repositoryClient: RepositoryService,
    val projectClient: ProjectService
) : PermissionService {

    private val permHelper by lazy { PermissionHelper(userDao, roleRepository, permissionDao, personalPathDao) }

    override fun deletePermission(id: String): Boolean {
        logger.info("delete  permission  repoName: [$id]")
        permissionDao.deleteById(id)
        return true
    }

    override fun listExternalRoleByProject(projectId: String, source: RoleSource): List<ExternalRoleResult> {
        return emptyList()
    }

    override fun getPermission(permissionId: String): Permission? {
        val result = permissionDao.findFirstById(permissionId) ?: run {
            return null
        }
        return PermRequestUtil.convToPermission(result)
    }

    override fun getPathCheckConfig(): Boolean {
        return true
    }

    override fun listNoPermissionPath(userId: String, projectId: String, repoName: String): List<String> {
        val user = userDao.findFirstByUserId(userId) ?: return emptyList()
        if (user.admin || isUserLocalProjectAdmin(userId, projectId)) {
            return emptyList()
        }
        val projectPermission = permissionDao.listByResourceAndRepo(ResourceType.NODE, projectId, repoName)
        val configPath = permHelper.getPermissionPathFromConfig(userId, user.roles, projectPermission, false)
        val personalPath = personalPathDao.listByProjectAndRepoAndExcludeUser(userId, projectId, repoName)
            .map { it.fullPath }
        return (configPath + personalPath).distinct()
    }

    override fun listPermissionPath(userId: String, projectId: String, repoName: String): List<String>? {
        val user = userDao.findFirstByUserId(userId) ?: return emptyList()
        if (user.admin || isUserLocalProjectAdmin(userId, projectId)) {
            return null
        }
        val permission = permissionDao.listByResourceAndRepo(ResourceType.NODE, projectId, repoName)
        val configPath = permHelper.getPermissionPathFromConfig(userId, user.roles, permission, true).toMutableList()
        val personalPath = personalPathDao.findOneByProjectAndRepo(userId, projectId, repoName)
        if (personalPath != null) {
            configPath.add(personalPath.fullPath)
        }
        return configPath.distinct()
    }

    override fun updatePermissionDeployInRepo(request: UpdatePermissionDeployInRepoRequest): Boolean {
        logger.info("update permission deploy in repo, create [$request]")
        permHelper.checkPermissionExist(request.permissionId)
        return permHelper.updatePermissionById(request.permissionId, TPermission::includePattern.name, request.path)
                && permHelper.updatePermissionById(request.permissionId, TPermission::users.name, request.users)
                && permHelper.updatePermissionById(request.permissionId, TPermission::permName.name, request.name)
                && permHelper.updatePermissionById(request.permissionId, TPermission::roles.name, request.roles)
    }

    override fun getOrCreatePersonalPath(projectId: String, repoName: String, userId: String): String {
        val personalPath = "${defaultPersonalPrefix}/$userId"
        personalPathDao.findOneByProjectAndRepo(userId, projectId, repoName) ?: run {
            logger.info("personal path [$projectId, $repoName, $personalPath ] not exist , create")
            val personalPathData =
                TPersonalPath(
                    projectId = projectId,
                    repoName = repoName,
                    userId = userId,
                    fullPath = personalPath
                )
            try {
                personalPathDao.insert(personalPathData)
            } catch (exception: RuntimeException) {
                logger.error("create personal path error [$projectId, $repoName, $personalPath ,$exception]")
            }

        }
        return personalPath
    }

    override fun checkRepoAccessControl(projectId: String, repoName: String): Boolean {
        val result = repoAuthConfigDao.findOneByProjectRepo(projectId, repoName) ?: return false
        return result.accessControlMode != null && result.accessControlMode == AccessControlMode.STRICT
    }

    override fun checkPlatformPermission(request: CheckPermissionRequest): Boolean {
        with(request) {
            val platform = accountDao.findOneByAppId(appId!!) ?: return false
            // 非平台账号
            if (!permHelper.isPlatformApp(platform)) return false
            // 不限制scope
            if (platform.scope == null) return true
            // 平台账号，限制scope
            if (!platform.scope!!.contains(resourceType)) return false
            // 校验平台账号权限范围
            when (resourceType) {
                ResourceType.PROJECT -> {
                    return permHelper.checkPlatformProject(projectId, platform.scopeDesc)
                }
                else -> return true
            }
        }
    }

    override fun listPermissionRepo(projectId: String, userId: String, appId: String?): List<String> {
        logger.debug("list repo permission request : [$projectId, $userId] ")
        val user = userDao.findFirstByUserId(userId) ?: run {
            throw ErrorCodeException(AuthMessageCode.AUTH_USER_NOT_EXIST)
        }
        val roles = user.roles

        // 用户为系统管理员、项目管理员、项目用户
        if (user.admin || isUserLocalProjectAdmin(userId, projectId) || isUserLocalProjectUser(userId, projectId)) {
            return getAllRepoByProjectId(projectId)
        }

        val repoList = mutableListOf<String>()

        // 非管理员用户关联权限
        repoList.addAll(permHelper.getNoAdminUserRepo(projectId, userId))
        if (roles.isEmpty()) return repoList.distinct()

        val noAdminRole = mutableListOf<String>()

        // 仓库管理员角色关联权限
        val roleList = roleRepository.findByProjectIdAndTypeAndAdminAndIdIn(
            projectId = projectId, type = RoleType.REPO, admin = true, ids = roles
        )
        roleList.forEach {
            if (it.admin && it.repoName != null) {
                repoList.add(it.repoName!!)
            } else {
                noAdminRole.add(it.id!!)
            }
        }

        // 非仓库管理员角色关联权限
        repoList.addAll(permHelper.getNoAdminRoleRepo(projectId, noAdminRole))

        return repoList.distinct()
    }

    override fun listPermission(projectId: String, repoName: String?): List<Permission> {
        logger.debug("list  permission  projectId: [$projectId], repoName: [$repoName]")
        repoName?.let {
            return permissionDao.listByResourceAndRepo(ResourceType.REPO, projectId, repoName)
                .map { PermRequestUtil.convToPermission(it) }
        }
        return permissionDao.listByResourceAndProject(ResourceType.PROJECT, projectId)
            .map { PermRequestUtil.convToPermission(it) }
    }

    override fun listPermission(projectId: String, repoName: String?, resourceType: String): List<Permission> {
        logger.debug("list  permission  projectId: [$projectId , $repoName, $resourceType]")
        repoName?.let {
            return permissionDao.listByResourceAndRepo(ResourceType.lookup(resourceType), projectId, repoName).map {
                PermRequestUtil.convToPermission(it)
            }
        }
        return permissionDao.listByResourceAndProject(ResourceType.PROJECT, projectId).map {
            PermRequestUtil.convToPermission(it)
        }
    }

    override fun listBuiltinPermission(projectId: String, repoName: String): List<Permission> {
        logger.debug("list  builtin permission  projectId: [$projectId], repoName: [$repoName]")
        val repoAdmin = getOnePermission(projectId, repoName, AUTH_BUILTIN_ADMIN, listOf(PermissionAction.MANAGE))
        val repoUser = getOnePermission(
            projectId,
            repoName,
            AUTH_BUILTIN_USER,
            listOf(PermissionAction.WRITE, PermissionAction.DELETE, PermissionAction.UPDATE)
        )
        return listOf(repoAdmin, repoUser).map { PermRequestUtil.convToPermission(it) }
    }

    override fun listBuiltinPermissionNoBack(projectId: String, repoName: String) {
        logger.debug("list  builtin permission  projectId: [$projectId], repoName: [$repoName]")
        getOnePermissionNoBack(projectId, repoName, AUTH_BUILTIN_ADMIN, listOf(PermissionAction.MANAGE))
        getOnePermissionNoBack(
            projectId,
            repoName,
            AUTH_BUILTIN_USER,
            listOf(PermissionAction.WRITE, PermissionAction.DELETE, PermissionAction.UPDATE)
        )
    }

    override fun isAdmin(userId: String, projectId: String?, tenantId: String?): Boolean {
        return userDao.findFirstByUserId(userId)?.admin ?: false
    }

    override fun deletePermissionData(projectId: String, repoName: String): Boolean {
        return true
    }

    override fun listNodePermission(projectId: String, repoName: String?): List<Permission> {
        if (repoName == null) {
            return permissionDao.listByResourceAndProject(ResourceType.NODE, projectId)
                .map { PermRequestUtil.convToPermission(it) }
        }
        return permissionDao.listByResourceAndRepo(ResourceType.NODE, projectId, repoName)
            .map { PermRequestUtil.convToPermission(it) }
    }

    override fun getUserAuthPaths(
        userId: String,
        projectId: String,
        repoNames: List<String>,
        action: PermissionAction
    ): Map<String, List<String>> {
        return repoNames.associateWith { listOf("/") }
    }

    override fun createPermission(request: CreatePermissionRequest): Boolean {
        logger.info("create  permission request : [$request]")
        // todo check request
        val permission = permissionDao.findPermissionByProject(
            request.permName,
            request.projectId,
            request.resourceType
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

    override fun updateIncludePath(request: UpdatePermissionPathRequest): Boolean {
        logger.info("update include path request :[$request]")
        with(request) {
            permHelper.checkPermissionExist(permissionId)
            return permHelper.updatePermissionById(permissionId, TPermission::includePattern.name, path)
        }
    }

    override fun updateExcludePath(request: UpdatePermissionPathRequest): Boolean {
        logger.info("update exclude path request :[$request]")
        with(request) {
            permHelper.checkPermissionExist(permissionId)
            return permHelper.updatePermissionById(permissionId, TPermission::excludePattern.name, path)
        }
    }

    override fun updateRepoPermission(request: UpdatePermissionRepoRequest): Boolean {
        logger.info("update repo permission request :  [$request]")
        with(request) {
            permHelper.checkPermissionExist(permissionId)
            return permHelper.updatePermissionById(permissionId, TPermission::repos.name, repos)
        }
    }

    override fun updatePermissionUser(request: UpdatePermissionUserRequest): Boolean {
        logger.info("update permission user request:[$request]")
        with(request) {
            checkPermissionExist(permissionId)
            findPermissionById(permissionId)?.let { permission ->
                if (permission.resourceType == ResourceType.PROJECT) {
                    deleteRepoPermissionUserId(permission, request.userId)
                }
            }
            return permHelper.updatePermissionById(permissionId, TPermission::users.name, userId)
        }
    }

    private fun deleteRepoPermissionUserId(permission: Permission, userList: List<String>) {
        val deleteUsers = permission.users.filter { !userList.contains(it) }
        if (deleteUsers.isEmpty()) {
            return
        }
        for (userId in deleteUsers) {
            userDao.findFirstByUserId(userId)?.let { tUser ->
                val result = permissionDao.listPermissionByUserRoles(permission.projectId!!, userId, tUser.roles)
                    .filter { it.id != permission.id }
                if (result.isNotEmpty()) {
                    return
                }
                val permissions = permissionDao.listByResourceAndProjectAndUserId(
                    ResourceType.REPO,
                    permission.projectId!!,
                    userId
                )
                for (p in permissions) {
                    permHelper.updatePermissionById(p.id!!, TPermission::users.name, p.users.filter { it != userId })
                }
            }
        }
    }

    override fun updatePermissionRole(request: UpdatePermissionRoleRequest): Boolean {
        logger.info("update permission role request:[$request]")
        with(request) {
            checkPermissionExist(permissionId)
            return permHelper.updatePermissionById(permissionId, TPermission::roles.name, rId)
        }
    }

    override fun updatePermissionDepartment(request: UpdatePermissionDepartmentRequest): Boolean {
        logger.info("update  permission department request:[$request]")
        with(request) {
            checkPermissionExist(permissionId)
            return permHelper.updatePermissionById(permissionId, TPermission::departments.name, departmentId)
        }
    }

    override fun updatePermissionAction(request: UpdatePermissionActionRequest): Boolean {
        logger.info("update permission action request:[$request]")
        with(request) {
            checkPermissionExist(permissionId)
            return permHelper.updatePermissionById(permissionId, TPermission::actions.name, actions)
        }
    }

    override fun updatePermissionById(id: String, key: String, value: Any): Boolean {
        return permHelper.updatePermissionById(id, key, value)
    }

    override fun updatePermissionById(id: String, request: UpdatePermissionRequest): Boolean {
        val update = Update()
        update.set(TPermission::permName.name, request.permName)
        update.set(TPermission::includePattern.name, request.includePattern)
        update.set(TPermission::updatedBy.name, request.updatedBy)
        update.set(TPermission::updateAt.name, request.updateAt)
        return permissionDao.updateById(id, update)
    }

    override fun checkPermission(request: CheckPermissionRequest): Boolean {
        logger.debug("check permission request : [$request] ")
        with(request) {
            if (uid == ANONYMOUS_USER) return false
            val user = userDao.findFirstByUserId(uid) ?: run {
                throw ErrorCodeException(AuthMessageCode.AUTH_USER_NOT_EXIST)
            }
            // check user locked
            if (user.locked) return false
            // check user admin permission
            if (user.admin) return true
            // user is not system admin and projectId is null
            if (projectId == null) return false

            if (isUserLocalProjectAdmin(uid, projectId!!)) return true
            val context = CheckPermissionContext(
                userId = uid,
                roles = user.roles,
                resourceType = resourceType,
                action = action,
                projectId = projectId!!,
                repoName = repoName,
                path = path,
            )

            if (permHelper.isRepoOrNodePermission(resourceType)) {
                return checkLocalRepoOrNodePermission(context)
            }
        }
        return false
    }

    private fun isUserLocalProjectAdmin(userId: String, projectId: String): Boolean {
        return permHelper.isUserLocalProjectAdmin(userId, projectId)
    }

    private fun checkLocalRepoOrNodePermission(context: CheckPermissionContext): Boolean {
        // check role repo admin
        if (permHelper.checkRepoAdmin(context)) return true
        // check repo read action
        if (permHelper.checkRepoReadAction(context)) return true
        //  check project user
        val isProjectUser = isUserLocalProjectUser(context.userId, context.projectId)
        if (permHelper.checkProjectReadAction(context, isProjectUser)) return true
        // check node action
        if (needNodeCheck(context.projectId, context.repoName!!) && checkNodeAction(context, isProjectUser)) {
            return true
        }
        return false
    }

    private fun needNodeCheck(projectId: String, repoName: String): Boolean {
        val projectPermission = permissionDao.listByResourceAndRepo(ResourceType.NODE, projectId, repoName)
        val repoCheckConfig = repoAuthConfigDao.findOneByProjectRepo(projectId, repoName) ?: return false
        return projectPermission.isNotEmpty() && repoCheckConfig.accessControlMode != AccessControlMode.DEFAULT
    }

    private fun checkNodeAction(request: CheckPermissionContext, isProjectUser: Boolean): Boolean {
        with(request) {
            if (checkRepoAccessControl(projectId, repoName!!)) {
                return permHelper.checkNodeActionWithCtrl(request)
            }
            return permHelper.checkNodeActionWithOutCtrl(request, isProjectUser)
        }
    }

    override fun listPermissionProject(userId: String): List<String> {
        logger.debug("list permission project request : $userId ")
        if (userId.isEmpty()) return emptyList()
        val user = userDao.findFirstByUserId(userId) ?: run {
            return listOf()
        }
        // 用户为系统管理员
        if (user.admin) {
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

    fun listProjectPublicRepo(projectId: String): List<String> {
        return repositoryClient.listRepo(projectId).filter {
            it.public
        }.map { it.name }
    }

    fun listPublicRepo(projectId: String): List<String> {
        return repositoryClient.listRepo(projectId).filter {
            it.configuration.settings["system"] == true || it.public
        }.map { it.name }
    }

    override fun listPermissionRepo(
        projectId: String,
        userId: String,
        appId: String?,
        actions: List<PermissionAction>?,
        includePathAuthRepo: Boolean
    ): List<String> {
        logger.debug("list repo permission request : [$projectId, $userId] ")
        val user = userDao.findFirstByUserId(userId) ?: run {
            return listProjectPublicRepo(projectId)
        }

        // 用户为系统管理员
        if (user.admin) {
            return getAllRepoByProjectId(projectId)
        }

        val roles = user.roles

        // 用户为该项目成员
        if (permissionDao.listByResourceAndProjectAndUserId(
                projectId = projectId,
                userId = userId,
                resourceType = ResourceType.PROJECT
            ).isNotEmpty()
        ) {
            return getAllRepoByProjectId(projectId)
        }

        // 用户为该项目关联用户组成员
        if (permissionDao.listByResourceAndProjectAndRoles(
                projectId = projectId,
                roles = roles,
                resourceType = ResourceType.PROJECT
            ).isNotEmpty()
        ) {
            return getAllRepoByProjectId(projectId)
        }

        val repoList = mutableListOf<String>()

        // 非管理员用户关联权限
        repoList.addAll(getNoAdminUserRepo(projectId, userId))

        if (user.roles.isEmpty()) {
            return repoList.distinct()
        }

        val noAdminRole = mutableListOf<String>()

        // 仓库管理员角色关联权限
        val roleList = roleRepository.findByProjectIdAndTypeAndAdminAndIdIn(projectId, RoleType.REPO, true, roles)
        roleList.forEach {
            if (it.admin && it.repoName != null) {
                repoList.add(it.repoName!!)
            } else {
                noAdminRole.add(it.id!!)
            }
        }

        // 非仓库管理员角色关联权限
        repoList.addAll(getNoAdminRoleRepo(projectId, noAdminRole))

        return repoList.distinct()
    }

    fun getAllRepoByProjectId(projectId: String): List<String> {
        return repositoryClient.listRepo(projectId).map { it.name }
    }

    fun getNoAdminUserProject(userId: String): List<String> {
        val projectList = mutableListOf<String>()
        permissionDao.listByUserId(userId).forEach {
            if (it.actions.isNotEmpty() && it.projectId != null) {
                projectList.add(it.projectId!!)
            }
        }
        return projectList
    }

    fun getNoAdminRoleProject(roles: List<String>): List<String> {
        val project = mutableListOf<String>()
        if (roles.isNotEmpty()) {
            permissionDao.listByRole(roles).forEach {
                if (it.actions.isNotEmpty() && it.projectId != null) {
                    project.add(it.projectId!!)
                }
            }
        }
        return project
    }

    private fun getNoAdminUserRepo(projectId: String, userId: String): List<String> {
        val repoList = mutableListOf<String>()
        permissionDao.listByProjectIdAndUsers(projectId, userId).forEach {
            if (it.actions.isNotEmpty() && it.repos.isNotEmpty()) {
                repoList.addAll(it.repos)
            }
        }
        return repoList
    }

    fun getNoAdminRoleRepo(project: String, role: List<String>): List<String> {
        val repoList = mutableListOf<String>()
        if (role.isNotEmpty()) {
            permissionDao.listByProjectAndRoles(project, role).forEach {
                if (it.actions.isNotEmpty() && it.repos.isNotEmpty()) {
                    repoList.addAll(it.repos)
                }
            }
        }
        return repoList
    }

    override fun listProjectBuiltinPermission(projectId: String): List<Permission> {
        val projectManage = getProjectPermission(projectId, PROJECT_MANAGE_PERMISSION)
        val projectView = getProjectPermission(projectId, PROJECT_VIEW_PERMISSION)
        return listOf(projectManage, projectView).map { PermRequestUtil.convToPermission(it) }
    }

    override fun isProjectManager(userId: String): Boolean {
        val user = userDao.findFirstByUserId(userId) ?: return false
        if (user.admin) return true
        val roles = user.roles
        if (permissionDao.findAllByResourceTypeAndPermNameAndUser(
                ResourceType.PROJECT,
                PROJECT_MANAGE_PERMISSION,
                userId
            ).isNotEmpty()
        ) {
            return true
        }
        if (permissionDao.findAllByResourceTypeAndPermNameAndRolesIn(
                ResourceType.PROJECT,
                PROJECT_MANAGE_PERMISSION,
                roles
            ).isNotEmpty()
        ) {
            return true
        }
        return false
    }

    private fun findPermissionById(id: String): Permission? {
        return permissionDao.findFirstById(id)?.let {
            PermRequestUtil.convToPermission(it)
        }
    }

    override fun findOneByPermNameAndProjectIdAndResourceTypeAndRepos(
        permName: String,
        projectId: String?,
        resourceType: ResourceType,
        repo: String
    ): Permission? {
        return permissionDao.findOneByPermName(
            permName = permName,
            projectId = projectId!!,
            resourceType = resourceType,
            repoName = repo
        )?.let {
            PermRequestUtil.convToPermission(it)
        }
    }

    private fun checkPermissionExist(pId: String) {
        permissionDao.findFirstById(pId) ?: run {
            logger.warn("update permission repos [$pId]  not exist.")
            throw ErrorCodeException(AuthMessageCode.AUTH_PERMISSION_NOT_EXIST)
        }
    }

    private fun getOnePermissionNoBack(
        projectId: String,
        repoName: String,
        permName: String,
        actions: List<PermissionAction>
    ) {
        val tPermission = permissionDao.findOneByPermName(
            projectId,
            repoName,
            permName,
            ResourceType.REPO
        )
        if (tPermission == null) {
            val request = TPermission(
                projectId = projectId,
                repos = listOf(repoName),
                permName = permName,
                actions = actions,
                resourceType = ResourceType.REPO,
                createAt = LocalDateTime.now(),
                updateAt = LocalDateTime.now(),
                createBy = AUTH_ADMIN,
                updatedBy = AUTH_ADMIN
            )
            permissionDao.insert(request)
        }
    }

    fun getOnePermission(
        projectId: String,
        repoName: String,
        permName: String,
        actions: List<PermissionAction>
    ): TPermission {
        val tPermission = permissionDao.findOneByPermName(
            projectId,
            repoName,
            permName,
            ResourceType.REPO
        )
        if (tPermission == null) {
            val request = TPermission(
                projectId = projectId,
                repos = listOf(repoName),
                permName = permName,
                actions = actions,
                resourceType = ResourceType.REPO,
                createAt = LocalDateTime.now(),
                updateAt = LocalDateTime.now(),
                createBy = AUTH_ADMIN,
                updatedBy = AUTH_ADMIN
            )
            logger.info("permission not exist, create [$request]")
            permissionDao.insert(request)
        }
        return permissionDao.findOneByPermName(
            projectId,
            repoName,
            permName,
            ResourceType.REPO
        )!!
    }

    private fun getProjectPermission(projectId: String, permName: String): TPermission {
        permissionDao.findPermissionByProject(
            permName,
            projectId,
            ResourceType.PROJECT
        ) ?: run {
            val request = TPermission(
                projectId = projectId,
                permName = permName,
                actions = getProjectBuiltinPermissionAction(permName),
                resourceType = ResourceType.PROJECT,
                createAt = LocalDateTime.now(),
                updateAt = LocalDateTime.now(),
                createBy = AUTH_ADMIN,
                updatedBy = AUTH_ADMIN
            )
            logger.info("permission not exist, create [$request]")
            permissionDao.insert(request)
        }
        return permissionDao.findPermissionByProject(
            permName,
            projectId,
            ResourceType.PROJECT
        )!!
    }

    private fun isUserLocalProjectUser(userId: String, projectId: String): Boolean {
        val user = userDao.findFirstByUserId(userId) ?: run {
            throw ErrorCodeException(AuthMessageCode.AUTH_USER_NOT_EXIST)
        }
        val roles = user.roles
        return roleRepository.findAllById(roles)
            .any { role -> role.projectId == projectId && role.roleId == PROJECT_VIEWER_ID }
    }

    private fun getProjectBuiltinPermissionAction(permName: String): List<PermissionAction> {
        return when (permName) {
            PROJECT_MANAGE_PERMISSION -> listOf(PermissionAction.MANAGE)
            else -> listOf(PermissionAction.READ)
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(CpackPermissionServiceImpl::class.java)
        private const val defaultPersonalPrefix = "/Personal"
    }
}
