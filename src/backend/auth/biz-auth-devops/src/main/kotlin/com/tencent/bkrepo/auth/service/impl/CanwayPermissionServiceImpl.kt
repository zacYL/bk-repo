package com.tencent.bkrepo.auth.service.impl

import com.tencent.bkrepo.auth.ciApi
import com.tencent.bkrepo.auth.ciPermission
import com.tencent.bkrepo.auth.constant.AUTH_BUILTIN_ADMIN
import com.tencent.bkrepo.auth.constant.AUTH_BUILTIN_USER
import com.tencent.bkrepo.auth.message.AuthMessageCode
import com.tencent.bkrepo.auth.model.TPermission
import com.tencent.bkrepo.auth.pojo.RegisterResourceRequest
import com.tencent.bkrepo.auth.pojo.enums.PermissionAction
import com.tencent.bkrepo.auth.pojo.enums.ResourceType
import com.tencent.bkrepo.auth.pojo.enums.RoleType
import com.tencent.bkrepo.auth.pojo.permission.CheckPermissionRequest
import com.tencent.bkrepo.auth.pojo.permission.CreatePermissionRequest
import com.tencent.bkrepo.auth.pojo.permission.Permission
import com.tencent.bkrepo.auth.pojo.permission.UpdatePermissionActionRequest
import com.tencent.bkrepo.auth.pojo.permission.UpdatePermissionDepartmentRequest
import com.tencent.bkrepo.auth.pojo.permission.UpdatePermissionPathRequest
import com.tencent.bkrepo.auth.pojo.permission.UpdatePermissionRepoRequest
import com.tencent.bkrepo.auth.pojo.permission.UpdatePermissionRoleRequest
import com.tencent.bkrepo.auth.pojo.permission.UpdatePermissionUserRequest
import com.tencent.bkrepo.auth.repository.PermissionRepository
import com.tencent.bkrepo.auth.repository.RoleRepository
import com.tencent.bkrepo.auth.repository.UserRepository
import com.tencent.bkrepo.auth.service.DevopsUserService
import com.tencent.bkrepo.auth.util.query.PermissionQueryHelper
import com.tencent.bkrepo.common.api.constant.ANONYMOUS_USER
import com.tencent.bkrepo.common.api.exception.ErrorCodeException
import com.tencent.bkrepo.common.api.util.readJsonString
import com.tencent.bkrepo.common.devops.client.DevopsClient
import com.tencent.bkrepo.common.devops.conf.DevopsConf
import com.tencent.bkrepo.common.devops.enums.InstanceType
import com.tencent.bkrepo.common.devops.pojo.response.CanwayResponse
import com.tencent.bkrepo.common.devops.service.BkUserService
import com.tencent.bkrepo.common.devops.util.http.CanwayHttpUtils
import com.tencent.bkrepo.common.security.exception.PermissionException
import com.tencent.bkrepo.common.service.util.HttpContextHolder
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
    private val devopsConf: DevopsConf,
    private val bkUserService: BkUserService,
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
    lateinit var devopsUserService: DevopsUserService

    @Autowired
    lateinit var devopsClient: DevopsClient

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

    override fun checkPermission(request: CheckPermissionRequest): Boolean {
        logger.info("check permission  request : [$request] ")

        if (request.uid == ANONYMOUS_USER) return false
        // 校验用户是否属于对应部门、用户组和已添加用户
        if (isBkrepoAdmin(request)) return true
        // 校验用户是否为CI 超级管理员或项目管理员
        if (isCIAdmin(request.uid, projectId = request.projectId)) return true
        if (checkCIReadPermission(request)) return true
        return canwayCheckPermission(request)
    }

    fun canwayCheckPermission(request: CheckPermissionRequest): Boolean {
        logger.debug("check permission  request : [$request] ")

        val user = userRepository.findFirstByUserId(request.uid) ?: run {
            throw ErrorCodeException(AuthMessageCode.AUTH_USER_NOT_EXIST)
        }

        // check user repo admin
        if (checkRepoUserAdmin(request)) return true

        // check role project admin
        if (checkProjectAdmin(request, user.roles)) return true

        // check role repo admin
        if (checkRepoAdmin(request, user.roles)) return true

        // check project action
        if (checkProjectAction(request, user.roles)) return true

        // 查询用户在CI所属组织/部门
        val departments = devopsClient.departmentsByUserId(request.uid)?.map { it.id }

        // 查询项目下所有用户组
        val ciRoles = request.projectId?.let { devopsClient.groupsByProjectId(it)
                ?.filter { canwayGroup -> canwayGroup.users.contains(request.uid) }
                ?.map { canwayGroup -> canwayGroup.id } } ?: emptyList()

        // check repo action
        return checkRepoAction(request, ciRoles, departments)
    }

    override fun checkRepoAction(request: CheckPermissionRequest, roles: List<String>, departments: List<String>?): Boolean {
        with(request) {
            if (resourceType == ResourceType.REPO && repoName != null) {
                val query = PermissionQueryHelper.buildPermissionCheck(
                        projectId!!, repoName!!, uid, action, resourceType, roles, departments
                )
                val result = mongoTemplate.count(query, TPermission::class.java)
                if (result != 0L) return true
            }
        }
        return false
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
        val ciProjectGroups = devopsUserService.groupsByProjectId(projectId)?.map { it.id }
        return listOf(repoAdmin, repoUser).map { transferCIPermission(it, ciProjectGroups) }
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

        // 用户为系统管理员
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
        ) return getAllRepoByProjectId(projectId)

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
        logger.debug("check permission  request : [$request] ")
        val user = userRepository.findFirstByUserId(request.uid) ?: run {
            throw ErrorCodeException(AuthMessageCode.AUTH_USER_NOT_EXIST)
        }

        // check user admin permission
        return user.admin
    }

    override fun updatePermissionDepartment(request: UpdatePermissionDepartmentRequest): Boolean {
        val webRequest = HttpContextHolder.getRequest()
        val api = webRequest.requestURI.removePrefix("/").removePrefix("web/")
        if (api.startsWith("api", ignoreCase = true)) {
            if (!checkPermissionById(request.permissionId)) throw PermissionException()
        }
        return super.updatePermissionDepartment(request)
    }

    override fun updatePermissionUser(request: UpdatePermissionUserRequest): Boolean {
        val webRequest = HttpContextHolder.getRequest()
        val requestUri = webRequest.requestURI
        logger.info("CanwayPermissionService accept : $requestUri, $request")
        val api = requestUri.removePrefix("/").removePrefix("web/")
        if (api.startsWith("api", ignoreCase = true)) {
            if (!checkPermissionById(request.permissionId)) throw PermissionException()
        }
        return super.updatePermissionUser(request)
    }

    override fun updatePermissionRole(request: UpdatePermissionRoleRequest): Boolean {
        val webRequest = HttpContextHolder.getRequest()
        val api = webRequest.requestURI.removePrefix("/").removePrefix("web/")
        if (api.startsWith("api", ignoreCase = true)) {
            if (!checkPermissionById(request.permissionId)) throw PermissionException()
        }
        return super.updatePermissionRole(request)
    }

    override fun updatePermissionAction(request: UpdatePermissionActionRequest): Boolean {
        val webRequest = HttpContextHolder.getRequest()
        val api = webRequest.requestURI.removePrefix("/").removePrefix("web/")
        if (api.startsWith("api", ignoreCase = true)) {
            if (!checkPermissionById(request.permissionId)) throw PermissionException()
        }
        return super.updatePermissionAction(request)
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
            logger.info("check user $userId is CI admin of project $projectId")
            devopsClient.isAdmin(userId, InstanceType.PROJECT, projectId) ?: false
        } else if (projectId == null && tenantId != null) {
            logger.info("check user $userId is CI admin of tenant $tenantId")
            devopsClient.isAdmin(userId, InstanceType.TENANT, tenantId) ?: false
        } else {
            logger.info("check user $userId is CI super admin ")
            devopsClient.isAdmin(userId, InstanceType.SUPER) ?: false
        }
    }

    /**
     * 查询用户是否为 CI 项目下用户
     */
    @Suppress("TooGenericExceptionCaught")
    private fun isCIProjectUser(userId: String, projectId: String): Boolean {
        val requestUrl = getPermissionUrl(String.format(ciUsersByProjectApi, projectId))
        val users = try {
            val responseContent = CanwayHttpUtils.doGet(requestUrl).content
            responseContent.readJsonString<CanwayResponse<List<String>>>().data
        } catch (e: Exception) {
            logger.error("query CI project users failed: [$requestUrl]", e)
            null
        }
        return users?.contains(userId) ?: false
    }

    private fun checkPermissionById(permissionId: String): Boolean {
        val userId = bkUserService.getBkUser()
        val tPermission = permissionRepository.findFirstById(permissionId)!!
        logger.info("CanwayPermissionService found tPermission: $tPermission")
        val checkPermissionRequest = CheckPermissionRequest(
            uid = userId,
            resourceType = ResourceType.REPO,
            action = PermissionAction.MANAGE,
            projectId = tPermission.projectId,
            repoName = tPermission.repos.first()
        )
        return checkPermission(checkPermissionRequest)
    }

    /**
     * 校验用户是否是CI 项目下用户，如果是且action == [PermissionAction.READ] 鉴权通过
     */
    private fun checkCIReadPermission(request: CheckPermissionRequest): Boolean {
        val uid = request.uid
        val projectId = request.projectId ?: return false
        val resourceType = request.resourceType
        val action = request.action

        return if (resourceType == ResourceType.REPO && action == PermissionAction.READ) {
            isCIProjectUser(uid, projectId)
        } else false
    }

    private fun getPermissionUrl(uri: String): String {
        val devopsHost = devopsConf.devopsHost
        return "${devopsHost.removeSuffix("/")}$ciPermission$ciApi$uri"
    }

    companion object {
        val logger: Logger = LoggerFactory.getLogger(CanwayPermissionServiceImpl::class.java)
        const val ciUsersByProjectApi = "/service/resource_instance/view/project/%s"
        fun transferCIPermission(permission: TPermission, ciProjectGroups: List<String>?): Permission {
            val roles = permission.roles.filter { ciProjectGroups?.contains(it) == true }
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
                departments = permission.departments,
                actions = permission.actions,
                createBy = permission.createBy,
                createAt = permission.createAt,
                updatedBy = permission.updatedBy,
                updateAt = permission.updateAt
            )
        }
    }
}
