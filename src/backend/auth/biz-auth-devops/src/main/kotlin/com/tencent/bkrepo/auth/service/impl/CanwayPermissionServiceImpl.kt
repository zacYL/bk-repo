package com.tencent.bkrepo.auth.service.impl

import com.tencent.bkrepo.auth.constant.AUTH_BUILTIN_ADMIN
import com.tencent.bkrepo.auth.constant.AUTH_BUILTIN_USER
import com.tencent.bkrepo.auth.job.BkDepartmentCache
import com.tencent.bkrepo.auth.message.AuthMessageCode
import com.tencent.bkrepo.auth.model.TPermission
import com.tencent.bkrepo.auth.pojo.RegisterResourceRequest
import com.tencent.bkrepo.auth.pojo.enums.PermissionAction
import com.tencent.bkrepo.auth.pojo.enums.ResourceType
import com.tencent.bkrepo.auth.pojo.enums.RoleType
import com.tencent.bkrepo.auth.pojo.permission.CheckPermissionRequest
import com.tencent.bkrepo.auth.pojo.permission.CreatePermissionRequest
import com.tencent.bkrepo.auth.pojo.permission.Permission
import com.tencent.bkrepo.auth.pojo.permission.UpdatePermissionPathRequest
import com.tencent.bkrepo.auth.pojo.permission.UpdatePermissionRepoRequest
import com.tencent.bkrepo.auth.repository.PermissionRepository
import com.tencent.bkrepo.auth.repository.RoleRepository
import com.tencent.bkrepo.auth.repository.UserRepository
import com.tencent.bkrepo.auth.util.query.PermissionQueryHelper
import com.tencent.bkrepo.common.api.constant.ANONYMOUS_USER
import com.tencent.bkrepo.common.api.exception.ErrorCodeException
import com.tencent.bkrepo.common.devops.BK_WHITELIST_USER
import com.tencent.bkrepo.common.devops.client.BkClient
import com.tencent.bkrepo.common.devops.client.DevopsClient
import com.tencent.bkrepo.common.devops.util.http.DevopsHttpUtils
import com.tencent.bkrepo.repository.api.ProjectClient
import com.tencent.bkrepo.repository.api.RepositoryClient
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.mongodb.core.MongoTemplate
import java.time.LocalDateTime

class CanwayPermissionServiceImpl(
    private val userRepository: UserRepository,
    private val roleRepository: RoleRepository,
    private val permissionRepository: PermissionRepository,
    private val mongoTemplate: MongoTemplate,
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

    @Autowired
    lateinit var devopsClient: DevopsClient

    @Autowired
    lateinit var bkClient: BkClient

    @Autowired
    lateinit var bkDepartmentCache: BkDepartmentCache

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

    /**
     * 1. 制品库或CI超级管理员
     * 2. 项目管理员 -- 与CI 集成时项目相关权限不在存在，比如创建项目，添加项目用户等操作都被隐藏了，
     * CI 集成时web 端所有操作都是仓库级权限，所以CI 项目管理员 也等于制品库管理员
     */
    override fun checkPermission(request: CheckPermissionRequest): Boolean {
        logger.info("check permission  request : [$request] ")
        if (request.uid == ANONYMOUS_USER) return false
        // 校验用户是否为制品库管理员
        if (isBkrepoAdmin(request)) return true
        // 校验用户是否为CI 超级管理员或项目管理员
        if (isCIAdmin(request.uid, projectId = request.projectId)) return true
        // 如果用户是CI 项目用户, 且action == [PermissionAction.READ] 鉴权通过
        if (checkCIReadPermission(request)) return true
        return canwayCheckPermission(request)
    }

    fun canwayCheckPermission(request: CheckPermissionRequest): Boolean {
        val user = userRepository.findFirstByUserId(request.uid) ?: run {
            throw ErrorCodeException(AuthMessageCode.AUTH_USER_NOT_EXIST)
        }

        // 查询用户是否为仓库管理员
        if (checkRepoUserAdmin(request)) return true

        // 查询用户的所属组是否为项目管理员
        if (checkProjectAdmin(request, user.roles)) return true
        // 查询用户的所属组是否为仓库管理员
        if (checkRepoAdmin(request, user.roles)) return true

        // 查询用户的所属组是否为项目用户
        if (checkProjectAction(request, user.roles)) return true

        // 查询用户在CI所属组织/部门
        val departments = devopsClient.departmentsByUserId(request.uid)?.map { it.id }
        // 查询用户在CI所属用户组
        val ciRoles = request.projectId?.let { devopsClient.groupsByProjectId(it) }
        val roles = mutableListOf<String>().apply {
            addAll(user.roles)
            addAll(ciRoles?.filter { it.users.contains(request.uid) }?.map { it.id } ?: emptyList())
        }.distinct().filter { it.isNotBlank() }

        // check repo action
        return checkRepoAction(request, roles, departments)
    }

    override fun checkRepoAction(
        request: CheckPermissionRequest,
        roles: List<String>,
        departments: List<String>?
    ): Boolean {
        with(request) {
            if (resourceType == ResourceType.REPO && repoName != null) {
                val filterDepartments = filterUserDepartment(departments, projectId!!)
                // 是否为仓库的管理者
                val manageQuery = PermissionQueryHelper.buildPermissionCheck(
                        projectId!!,
                        repoName!!,
                        uid,
                        PermissionAction.MANAGE,
                        resourceType,
                        roles,
                        filterDepartments
                )
                val manageResult = mongoTemplate.count(manageQuery, TPermission::class.java)
                if (manageResult != 0L) return true
                // 是否为仓库使用者
                val userQuery = PermissionQueryHelper.buildPermissionCheck(
                        projectId!!,
                        repoName!!,
                        uid,
                        action,
                        resourceType,
                        roles,
                        filterDepartments
                )
                val userResult = mongoTemplate.count(userQuery, TPermission::class.java)
                if (userResult != 0L) return true
            }
        }
        return false
    }

    /**
     * 过滤用户所属部门中没有在当前项目被授权的部门
     */
    private fun filterUserDepartment(departments: List<String>?, projectId: String): List<String>? {
        val ciDepartments = devopsClient.departmentsByProjectId(projectId)?.map { it.id } ?: emptyList()
        return departments?.filter { bkDepartmentCache.isParentDepartment(ciDepartments, it) }
    }

    override fun createPermission(request: CreatePermissionRequest): Boolean {
        logger.info("create  permission request : [$request]")
        val permission = permissionRepository.findOneByPermNameAndProjectIdAndResourceType(
            request.permName,
            request.projectId,
            request.resourceType
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
                createBy = request.createBy,
                createAt = LocalDateTime.now(),
                updatedBy = request.updatedBy,
                updateAt = LocalDateTime.now(),
                departments = request.departments
            )
        )
        result.id?.let { return true }
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

    override fun registerResource(request: RegisterResourceRequest) {
        return
    }

    override fun listBuiltinPermission(projectId: String, repoName: String): List<Permission> {
        val repoAdmin = super.getOnePermission(projectId, repoName, AUTH_BUILTIN_ADMIN, listOf(PermissionAction.MANAGE))
        val repoUser = super.getOnePermission(
            projectId,
            repoName,
            AUTH_BUILTIN_USER,
            listOf(PermissionAction.WRITE, PermissionAction.DELETE, PermissionAction.UPDATE)
        )
        // 过滤CI中的角色
        val ciProjectGroups = devopsClient.groupsByProjectId(projectId)?.map { it.id }
        // 查询蓝鲸全部部门
        val allDepartments = bkClient.allDepartmentIds(
            bkUsername = BK_WHITELIST_USER,
            bkToken = DevopsHttpUtils.getBkToken()
        ).map { it.toString() }
        logger.debug("$allDepartments")
        val projectDepartments = devopsClient.departmentsByProjectId(projectId)?.map { it.id }?.distinct()
        logger.debug("$projectDepartments")
        // 过滤蓝鲸中不存在的部门
        val departments = projectDepartments?.filter { allDepartments.contains(it) } ?: emptyList()
        logger.debug("$departments")
        return listOf(repoAdmin, repoUser).map {
            transferCIPermission(it, ciProjectGroups, departments, allDepartments)
        }
    }

    private fun checkPermissionExist(pId: String) {
        permissionRepository.findFirstById(pId) ?: run {
            logger.warn("update permission repos [$pId]  not exist.")
            throw ErrorCodeException(AuthMessageCode.AUTH_PERMISSION_NOT_EXIST)
        }
    }

    override fun isAdmin(userId: String, projectId: String?, tenantId: String?): Boolean {
        if (super.isAdmin(userId, null, null)) return true
        // CI超级管理员
        if (isCIAdmin(userId)) return true
        // CI项目管理员
        if (isCIAdmin(userId, projectId = projectId)) return true
        // CI租户管理员
        if (isCIAdmin(userId, tenantId = tenantId)) return true
        return false
    }

    override fun listPermissionProject(userId: String): List<String> {
        logger.debug("list permission project request : $userId ")
        if (userId.isEmpty()) return emptyList()
        val user = userRepository.findFirstByUserId(userId) ?: run {
            return listOf()
        }
        // 用户为系统管理员
        if (user.admin) {
            return projectClient.listProject().data?.map { it.name } ?: emptyList()
        }

        // 用户为CI 管理员
        if (isCIAdmin(user.userId)) {
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

    override fun listPermissionRepo(projectId: String, userId: String, appId: String?): List<String> {
        logger.debug("list repo permission request : [$projectId, $userId] ")
        val user = userRepository.findFirstByUserId(userId) ?: run {
            return listProjectPublicRepo(projectId)
        }
        // 用户是否为DevOps管理员
        if (isCIAdmin(userId,projectId)){
            return getAllRepoByProjectId(projectId)
        }

        // 用户为制品库系统管理员
        if (user.admin) {
            return getAllRepoByProjectId(projectId)
        }

        // 用户为CI 项目成员
        if (isCIProjectUser(user.userId, projectId)) {
            return getAllRepoByProjectId(projectId)
        }

        val roles = user.roles

        // 用户为项目管理员或项目成员
        if (roles.isNotEmpty() && roleRepository.findByProjectIdAndTypeAndIdIn(
                projectId,
                RoleType.PROJECT,
                roles
            ).isNotEmpty()
        ) {
            return getAllRepoByProjectId(projectId)
        }

        // 用户为该项目成员
        if (permissionRepository.findAllByProjectIdAndResourceTypeAndUsersIn(
                projectId = projectId,
                userId = userId,
                type = ResourceType.PROJECT
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

    private fun isBkrepoAdmin(request: CheckPermissionRequest): Boolean {
        val user = userRepository.findFirstByUserId(request.uid) ?: run {
            throw ErrorCodeException(AuthMessageCode.AUTH_USER_NOT_EXIST)
        }
        // check user admin permission
        return user.admin
    }

    /**
     * 用户是否为CI 管理员
     * @param userId: 用户id
     * @param projectId: 项目id
     * @param tenantId: 租户id
     */
    @Suppress("TooGenericExceptionCaught")
    private fun isCIAdmin(userId: String, projectId: String? = null, tenantId: String? = null): Boolean {
        return if (projectId != null && tenantId == null) {
            logger.info("check user $userId is CI project admin of [project: $projectId]")
            try {
                devopsClient.identifyProjectManageAuth(userId, projectId) ?: false
            }catch (e:Exception){
                //以防外一出现不存在项目ID的存在
                devopsClient.identifySystemManageAuth(userId) ?: false
            }
        } else if (projectId == null && tenantId != null) {
            logger.info("check user $userId is CI tenant admin of [tenant: $tenantId]")
            devopsClient.identifyTenantManageAuth(userId, tenantId) ?: false
        } else {
            logger.info("check user $userId is CI super admin")
            devopsClient.identifySystemManageAuth(userId) ?: false
        }
    }

    /**
     * 查询用户是否为 CI 项目下用户
     */
    @Suppress("TooGenericExceptionCaught")
    private fun isCIProjectUser(userId: String, projectId: String): Boolean {
        val projectUsers = devopsClient.usersByProjectId(projectId)?.map { it.userId }
        logger.info("[project: $projectId] contains the following members: $projectUsers")
        return projectUsers?.contains(userId) ?: false
    }

    private fun checkCIReadPermission(request: CheckPermissionRequest): Boolean {
        val projectId = request.projectId ?: return false
        return request.action == PermissionAction.READ && isCIProjectUser(request.uid, projectId)
    }

//    private fun getPermissionUrl(uri: String): String {
//        val devopsHost = devopsConf.devopsHost
//        return "${devopsHost.removeSuffix("/")}$ciPermission$ciApi$uri"
//    }

    private fun transferCIPermission(
        permission: TPermission,
        ciProjectGroups: List<String>?,
        ciDepartments: List<String>,
        allDepartments: List<String>
    ): Permission {
        val roles = permission.roles.filter { ciProjectGroups?.contains(it) == true }
        // 过滤CI项目下未授权的部门
        val departments = permission.departments.filter {
            allDepartments.contains(it) && bkDepartmentCache.isParentDepartment(ciDepartments, it)
        }
        return Permission(
            id = permission.id,
            resourceType = permission.resourceType,
            projectId = permission.projectId,
            permName = permission.permName,
            repos = permission.repos,
            includePattern = permission.includePattern,
            excludePattern = permission.excludePattern,
            users = permission.users,
            roles = roles,
            departments = departments,
            actions = permission.actions,
            createBy = permission.createBy,
            createAt = permission.createAt,
            updatedBy = permission.updatedBy,
            updateAt = permission.updateAt
        )
    }

    companion object {
        val logger: Logger = LoggerFactory.getLogger(CanwayPermissionServiceImpl::class.java)
    }
}
