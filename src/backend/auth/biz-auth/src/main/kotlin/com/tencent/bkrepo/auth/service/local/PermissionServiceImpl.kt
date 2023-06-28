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

package com.tencent.bkrepo.auth.service.local

import com.tencent.bkrepo.auth.constant.*
import com.tencent.bkrepo.auth.message.AuthMessageCode
import com.tencent.bkrepo.auth.model.TPermission
import com.tencent.bkrepo.auth.pojo.RegisterResourceRequest
import com.tencent.bkrepo.auth.pojo.enums.PermissionAction
import com.tencent.bkrepo.auth.pojo.enums.ResourceType
import com.tencent.bkrepo.auth.pojo.enums.RoleType
import com.tencent.bkrepo.auth.pojo.permission.*
import com.tencent.bkrepo.auth.repository.PermissionRepository
import com.tencent.bkrepo.auth.repository.RoleRepository
import com.tencent.bkrepo.auth.repository.UserRepository
import com.tencent.bkrepo.auth.service.PermissionService
import com.tencent.bkrepo.auth.util.query.PermissionQueryHelper
import com.tencent.bkrepo.common.api.constant.ANONYMOUS_USER
import com.tencent.bkrepo.common.api.exception.ErrorCodeException
import com.tencent.bkrepo.repository.api.ProjectClient
import com.tencent.bkrepo.repository.api.RepositoryClient
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.mongodb.core.MongoTemplate
import java.time.LocalDateTime

open class PermissionServiceImpl constructor(
    private val userRepository: UserRepository,
    private val roleRepository: RoleRepository,
    private val permissionRepository: PermissionRepository,
    private val mongoTemplate: MongoTemplate
) : PermissionService, AbstractServiceImpl(mongoTemplate, userRepository, roleRepository) {

    @Autowired
    lateinit var repositoryClient: RepositoryClient

    @Autowired
    lateinit var projectClient: ProjectClient

    override fun deletePermission(id: String): Boolean {
        logger.info("delete  permission  repoName: [$id]")
        permissionRepository.deleteById(id)
        return true
    }

    override fun listPermission(projectId: String, repoName: String?): List<Permission> {
        logger.debug("list  permission  projectId: [$projectId], repoName: [$repoName]")
        repoName?.let {
            return permissionRepository.findByResourceTypeAndProjectIdAndRepos(ResourceType.REPO, projectId, repoName)
                .map { transferPermission(it) }
        }
        return permissionRepository.findByResourceTypeAndProjectId(ResourceType.PROJECT, projectId)
            .map { transferPermission(it) }
    }

    override fun listBuiltinPermission(projectId: String, repoName: String): List<Permission> {
        logger.debug("list  builtin permission  projectId: [$projectId], repoName: [$repoName]")
        val repoAdmin = getOnePermission(projectId, repoName, AUTH_BUILTIN_ADMIN, listOf(PermissionAction.MANAGE))
        val repoUser = getOnePermission(
            projectId,
            repoName,
            AUTH_BUILTIN_USER,
            listOf(PermissionAction.WRITE, PermissionAction.READ, PermissionAction.DELETE, PermissionAction.UPDATE)
        )
//        val repoViewer = getOnePermission(projectId, repoName, AUTH_BUILTIN_VIEWER, listOf(PermissionAction.READ))
        return listOf(repoAdmin, repoUser).map { transferPermission(it) }
    }

    override fun listBuiltinPermissionNoBack(projectId: String, repoName: String) {
        TODO("Not yet implemented")
    }

    override fun isAdmin(userId: String, projectId: String?, tenantId: String?): Boolean {
        TODO("Not yet implemented")
    }

    override fun createPermission(request: CreatePermissionRequest): Boolean {
        logger.info("create  permission request : [$request]")
        // todo check request
        val permission = permissionRepository.findOneByPermNameAndProjectIdAndResourceType(
            request.permName, request.projectId, request.resourceType
        )
        permission?.let {
            logger.warn("create permission  [$request] is exist.")
            throw ErrorCodeException(AuthMessageCode.AUTH_DUP_PERMNAME)
        }
        val result = permissionRepository.insert(
            TPermission(
                resourceType = request.resourceType,
                projectId = request.projectId,
                permName = request.permName,
                repos = request.repos,
                includePattern = request.includePattern,
                excludePattern = request.excludePattern,
                users = request.users,
                roles = request.roles,
                actions = request.actions,
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
            checkPermissionExist(permissionId)
            return updatePermissionById(permissionId, TPermission::includePattern.name, path)
        }
    }

    override fun updateExcludePath(request: UpdatePermissionPathRequest): Boolean {
        logger.info("update exclude path request :[$request]")
        with(request) {
            checkPermissionExist(permissionId)
            return updatePermissionById(permissionId, TPermission::excludePattern.name, path)
        }
    }

    override fun updateRepoPermission(request: UpdatePermissionRepoRequest): Boolean {
        logger.info("update repo permission request :  [$request]")
        with(request) {
            checkPermissionExist(permissionId)
            return updatePermissionById(permissionId, TPermission::repos.name, repos)
        }
    }

    override fun updatePermissionUser(request: UpdatePermissionUserRequest): Boolean {
        logger.info("update permission user request:[$request]")
        with(request) {
            checkPermissionExist(permissionId)
            return updatePermissionById(permissionId, TPermission::users.name, userId)
        }
    }

    override fun updatePermissionRole(request: UpdatePermissionRoleRequest): Boolean {
        logger.info("update permission role request:[$request]")
        with(request) {
            checkPermissionExist(permissionId)
            return updatePermissionById(permissionId, TPermission::roles.name, rId)
        }
    }

    override fun updatePermissionDepartment(request: UpdatePermissionDepartmentRequest): Boolean {
        logger.info("update  permission department request:[$request]")
        with(request) {
            checkPermissionExist(permissionId)
            return updatePermissionById(permissionId, TPermission::departments.name, departmentId)
        }
    }

    override fun updatePermissionAction(request: UpdatePermissionActionRequest): Boolean {
        logger.info("update permission action request:[$request]")
        with(request) {
            checkPermissionExist(permissionId)
            return updatePermissionById(permissionId, TPermission::actions.name, actions)
        }
    }

    override fun checkPermission(request: CheckPermissionRequest): Boolean {
        logger.debug("check permission  request : [$request] ")

        if (request.uid == ANONYMOUS_USER) return false

        val user = userRepository.findFirstByUserId(request.uid) ?: run {
            throw ErrorCodeException(AuthMessageCode.AUTH_USER_NOT_EXIST)
        }

        // check user locked
        if (user.locked) {
            return false
        }
        // check user admin permission
        if (user.admin) return true

        // check user project admin
        if (checkProjectUserAdmin(request)) return true
        // check role project admin
        if (checkProjectAdmin(request, user.roles)) return true
        // check role repo admin
        if (checkRepoAdmin(request, user.roles)) return true
        // check repo action
        return checkAction(request, user.roles)
    }

    private fun checkProjectUserAdmin(request: CheckPermissionRequest): Boolean {
        request.projectId?.let {
            if (permissionRepository.findAllByResourceTypeAndPermNameAndProjectIdAndUsersIn(
                    ResourceType.PROJECT,
                    PROJECT_MANAGE_PERMISSION,
                    it,
                    listOf(request.uid)
                ).isNotEmpty()
            ) return true
        }
        return false
    }

    private fun checkProjectAdmin(request: CheckPermissionRequest, roles: List<String>): Boolean {
        var queryRoles = emptyList<String>()
        if (roles.isNotEmpty() && request.projectId != null) {
            queryRoles = roles.filter { !it.isNullOrEmpty() }.toList()
        }
        if (queryRoles.isEmpty()) {
            return false
        }
        val result = roleRepository.findByProjectIdAndTypeAndAdminAndIdIn(
            projectId = request.projectId!!, type = RoleType.PROJECT, admin = true, ids = queryRoles
        )
        if (result.isNotEmpty()) {
            return true
        }
        return false
    }

    private fun checkRepoAdmin(request: CheckPermissionRequest, roles: List<String>): Boolean {
        // check role repo admin
        var queryRoles = emptyList<String>()
        if (roles.isNotEmpty() && request.projectId != null && request.repoName != null) {
            queryRoles = roles.filter { !it.isNullOrEmpty() }.toList()

        }
        if (queryRoles.isEmpty()) return false

        val result = roleRepository.findByProjectIdAndTypeAndAdminAndRepoNameAndIdIn(
            projectId = request.projectId!!,
            type = RoleType.REPO,
            repoName = request.repoName!!,
            admin = true,
            ids = queryRoles
        )
        if (result.isNotEmpty()) return true
        return false
    }

    private fun checkAction(request: CheckPermissionRequest, roles: List<String>): Boolean {
        with(request) {
            if (resourceType == ResourceType.REPO && repoName != null) {
                val query = PermissionQueryHelper.buildPermissionCheck(
                    projectId!!, repoName!!, uid, action, resourceType, roles
                )
                val result = mongoTemplate.find(query, TPermission::class.java)
                if (result.isEmpty()) return false

                // result is not empty and path is null
                if (path == null) return true

                result.forEach {
                    if (checkIncludePattern(it.includePattern, path!!)) return true
                    if (!checkExcludePattern(it.excludePattern, path!!)) return false
                }
            }
        }
        return false
    }

    private fun checkIncludePattern(patternList: List<String>, path: String): Boolean {
        if (patternList.isEmpty()) return true
        patternList.forEach {
            if (path.contains(it)) return true
        }
        return false
    }

    private fun checkExcludePattern(patternList: List<String>, path: String): Boolean {
        if (patternList.isEmpty()) return true
        patternList.forEach {
            if (path.contains(it)) return false
        }
        return true
    }

    override fun listPermissionProject(userId: String): List<String> {
        logger.debug("list permission project request : $userId ")
        if (userId.isEmpty()) return emptyList()
        val user = userRepository.findFirstByUserId(userId) ?: run {
            return listOf()
//            throw AuthenticationException(AuthMessageCode.AUTH_USER_NOT_EXIST.name)
        }
        // 用户为系统管理员
        if (user.admin) {
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
                projectList.add(it.projectId)
            } else {
                noAdminRole.add(it.id!!)
            }
        }

        // 非管理员角色关联权限
        projectList.addAll(getNoAdminRoleProject(noAdminRole))

        return projectList.distinct()
    }

    private fun listProjectPublicRepo(projectId: String): List<String> {
        return repositoryClient.listRepo(projectId).data?.filter {
            it.public
        }?.map { it.name } ?: listOf()
    }

    override fun listPermissionRepo(
        projectId: String,
        userId: String,
        appId: String?,
        actions: List<PermissionAction>?
    ): List<String> {
        logger.debug("list repo permission request : [$projectId, $userId] ")
        val user = userRepository.findFirstByUserId(userId) ?: run {
            return listProjectPublicRepo(projectId)
//            throw ErrorCodeException(AuthMessageCode.AUTH_USER_NOT_EXIST)
        }

        // 用户为系统管理员
        if (user.admin) {
            return getAllRepoByProjectId(projectId)
        }

        val roles = user.roles

        // 用户为项目管理员
        if (roles.isNotEmpty() && roleRepository.findByProjectIdAndTypeAndAdminAndIdIn(
                projectId = projectId, type = RoleType.PROJECT, admin = true, ids = roles
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
        val roleList = roleRepository.findByProjectIdAndTypeAndAdminAndIdIn(
            projectId = projectId, type = RoleType.REPO, admin = true, ids = roles
        )
        roleList.forEach {
            if (it.admin && it.repoName != null) {
                repoList.add(it.repoName)
            } else {
                noAdminRole.add(it.id!!)
            }
        }

        // 非仓库管理员角色关联权限
        repoList.addAll(getNoAdminRoleRepo(projectId, noAdminRole))

        return repoList.distinct()
    }

    fun getAllRepoByProjectId(projectId: String): List<String> {
        return repositoryClient.listRepo(projectId).data?.map { it.name } ?: emptyList()
    }

    fun isUserLocalAdmin(userId: String): Boolean {
        val user = userRepository.findFirstByUserId(userId) ?: run {
            return false
        }
        return user.admin
    }

    private fun getNoAdminUserProject(userId: String): List<String> {
        val projectList = mutableListOf<String>()
        permissionRepository.findByUsers(userId).forEach {
            if (it.actions.isNotEmpty() && it.projectId != null) {
                projectList.add(it.projectId!!)
            }
        }
        return projectList
    }

    private fun getNoAdminRoleProject(roles: List<String>): List<String> {
        val project = mutableListOf<String>()
        if (roles.isNotEmpty()) {
            permissionRepository.findByRolesIn(roles).forEach {
                if (it.actions.isNotEmpty() && it.projectId != null) {
                    project.add(it.projectId!!)
                }
            }
        }
        return project
    }

    private fun getNoAdminUserRepo(projectId: String, userId: String): List<String> {
        val repoList = mutableListOf<String>()
        permissionRepository.findByProjectIdAndUsers(projectId, userId).forEach {
            if (it.actions.isNotEmpty() && it.repos.isNotEmpty()) {
                repoList.addAll(it.repos)
            }
        }
        return repoList
    }

    private fun getNoAdminRoleRepo(project: String, role: List<String>): List<String> {
        val repoList = mutableListOf<String>()
        if (role.isNotEmpty()) {
            permissionRepository.findByProjectIdAndRolesIn(project, role).forEach {
                if (it.actions.isNotEmpty() && it.repos.isNotEmpty()) {
                    repoList.addAll(it.repos)
                }
            }
        }
        return repoList
    }

    override fun registerResource(request: RegisterResourceRequest) {
        return
    }

    override fun listProjectBuiltinPermission(projectId: String): List<Permission> {
        val projectManage = getProjectPermission(projectId, PROJECT_MANAGE_PERMISSION)
        val projectView = getProjectPermission(projectId, PROJECT_VIEW_PERMISSION)
        return listOf(projectManage, projectView).map { transferPermission(it) }
    }

    override fun isProjectManager(userId: String): Boolean {
        val user = userRepository.findFirstByUserId(userId) ?: return false
        if (user.admin) return true
        val roles = user.roles
        if (permissionRepository.findAllByResourceTypeAndPermNameAndUsersIn(
                ResourceType.PROJECT,
                PROJECT_MANAGE_PERMISSION,
                listOf(userId)
            ).isNotEmpty()
        ) return true
        if (permissionRepository.findAllByResourceTypeAndPermNameAndRolesIn(
                ResourceType.PROJECT,
                PROJECT_MANAGE_PERMISSION,
                roles
            ).isNotEmpty()
        ) return true
        return false
    }

    override fun findPermissionById(id: String): Permission? {
        return permissionRepository.findFirstById(id)?.let {
            transferPermission(it)
        }
    }

    private fun checkPermissionExist(pId: String) {
        permissionRepository.findFirstById(pId) ?: run {
            logger.warn("update permission repos [$pId]  not exist.")
            throw ErrorCodeException(AuthMessageCode.AUTH_PERMISSION_NOT_EXIST)
        }
    }

    private fun getOnePermission(
        projectId: String, repoName: String, permName: String, actions: List<PermissionAction>
    ): TPermission {
        permissionRepository.findOneByProjectIdAndReposAndPermNameAndResourceType(
            projectId, repoName, permName, ResourceType.REPO
        ) ?: run {
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
            permissionRepository.insert(request)
        }
        return permissionRepository.findOneByProjectIdAndReposAndPermNameAndResourceType(
            projectId = projectId, repoName = repoName, permName = permName, resourceType = ResourceType.REPO
        )!!
    }

    private fun getProjectPermission(projectId: String, permName: String): TPermission {
        permissionRepository.findOneByPermNameAndProjectIdAndResourceType(
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
            permissionRepository.insert(request)
        }
        return permissionRepository.findOneByPermNameAndProjectIdAndResourceType(
            permName,
            projectId,
            ResourceType.PROJECT
        )!!
    }

    private fun getProjectBuiltinPermissionAction(permName: String): List<PermissionAction> {
        return when (permName) {
            PROJECT_MANAGE_PERMISSION -> listOf(PermissionAction.MANAGE)
            else -> listOf(PermissionAction.READ)
        }
    }

    private fun convActions(actions: List<PermissionAction>): List<String> {
        var result = mutableListOf<String>()
        actions.forEach {
            result.add(it.toString())
        }
        return result
    }

    companion object {
        private val logger = LoggerFactory.getLogger(PermissionServiceImpl::class.java)
    }
}
