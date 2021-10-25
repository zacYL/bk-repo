package com.tencent.bkrepo.auth.service.impl

import com.tencent.bkrepo.auth.ciPermission
import com.tencent.bkrepo.auth.constant.AUTH_BUILTIN_ADMIN
import com.tencent.bkrepo.auth.constant.AUTH_BUILTIN_USER
import com.tencent.bkrepo.auth.constant.AUTH_BUILTIN_VIEWER
import com.tencent.bkrepo.auth.message.AuthMessageCode
import com.tencent.bkrepo.auth.model.TPermission
import com.tencent.bkrepo.auth.pojo.enums.PermissionAction
import com.tencent.bkrepo.auth.pojo.enums.ResourceType
import com.tencent.bkrepo.auth.repository.PermissionRepository
import com.tencent.bkrepo.auth.repository.RoleRepository
import com.tencent.bkrepo.auth.repository.UserRepository
import com.tencent.bkrepo.auth.service.DepartmentService
import com.tencent.bkrepo.auth.ciApi
import com.tencent.bkrepo.auth.constant.AUTH_ADMIN
import com.tencent.bkrepo.auth.pojo.ActionCollection
import com.tencent.bkrepo.auth.pojo.RegisterResourceRequest
import com.tencent.bkrepo.auth.pojo.CanwayPermissionDepartment
import com.tencent.bkrepo.auth.pojo.CanwayPermissionResult
import com.tencent.bkrepo.auth.pojo.CanwayPermissionRole
import com.tencent.bkrepo.auth.pojo.enums.RoleType
import com.tencent.bkrepo.auth.pojo.permission.UpdatePermissionPathRequest
import com.tencent.bkrepo.auth.pojo.permission.UpdatePermissionActionRequest
import com.tencent.bkrepo.auth.pojo.permission.UpdatePermissionRepoRequest
import com.tencent.bkrepo.auth.pojo.permission.UpdatePermissionRoleRequest
import com.tencent.bkrepo.auth.pojo.permission.UpdatePermissionUserRequest
import com.tencent.bkrepo.auth.pojo.permission.UpdatePermissionDepartmentRequest
import com.tencent.bkrepo.auth.pojo.permission.Permission
import com.tencent.bkrepo.auth.pojo.permission.CheckPermissionRequest
import com.tencent.bkrepo.auth.pojo.permission.ListRepoPermissionRequest
import com.tencent.bkrepo.auth.pojo.permission.CreatePermissionRequest
import com.tencent.bkrepo.auth.service.local.PermissionServiceImpl
import com.tencent.bkrepo.common.api.exception.ErrorCodeException
import com.tencent.bkrepo.common.api.message.CommonMessageCode
import com.tencent.bkrepo.common.api.util.readJsonString
import com.tencent.bkrepo.common.api.util.toJsonString
import com.tencent.bkrepo.common.devops.api.conf.DevopsConf
import com.tencent.bkrepo.common.devops.api.enums.CanwayPermissionType
import com.tencent.bkrepo.common.devops.api.pojo.request.CanwayPermissionRequest
import com.tencent.bkrepo.common.devops.api.pojo.response.CanwayPermissionResponse
import com.tencent.bkrepo.common.devops.api.pojo.response.CanwayResponse
import com.tencent.bkrepo.common.devops.api.service.BkUserService
import com.tencent.bkrepo.common.devops.api.util.http.CanwayHttpUtils
import com.tencent.bkrepo.common.security.exception.PermissionException
import com.tencent.bkrepo.common.service.util.HttpContextHolder
import com.tencent.bkrepo.repository.api.ProjectClient
import com.tencent.bkrepo.repository.api.RepositoryClient
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.dao.DuplicateKeyException
import org.springframework.data.mongodb.core.MongoTemplate
import org.springframework.data.mongodb.core.query.Criteria
import org.springframework.data.mongodb.core.query.Query
import java.time.LocalDateTime
import java.util.stream.Collectors

class CanwayPermissionServiceImpl(
    private val userRepository: UserRepository,
    private val roleRepository: RoleRepository,
    private val permissionRepository: PermissionRepository,
    private val mongoTemplate: MongoTemplate,
    private val repositoryClient: RepositoryClient,
    private val devopsConf: DevopsConf,
    private val departmentService: DepartmentService,
    private val bkUserService: BkUserService,
    projectClient: ProjectClient
) : PermissionServiceImpl(
    userRepository,
    roleRepository,
    permissionRepository,
    mongoTemplate,
    repositoryClient,
    projectClient
) {

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
        // 校验用户是否属于对应部门、用户组和已添加用户
        if (isBkrepoAdmin(request)) return true
        if (checkUserHasProjectPermission(request.uid)) return true
        val canwayPermissionResult = canwayCheckPermission(request)
        if (!canwayPermissionResult.hasPermission) return false
        val action = PermissionAction.valueOf(request.action)
        if (ActionCollection.isCanwayAction(action)) {
            val actions = ActionCollection.getActionsByCanway(action)
            for (ac in actions) {
                val tempRequest = request.copy(
                    action = ac.name,
                    role = canwayPermissionResult.roles,
                    department = canwayPermissionResult.departments
                )
                val result = originCheckPermission(tempRequest)
                if (!result) return false
            }
            return true
        }
        return originCheckPermission(request)
    }

    override fun listBuiltinPermission(projectId: String, repoName: String): List<Permission> {
        logger.info("list  builtin permission  projectId: [$projectId], repoName: [$repoName]")
        val repoAdmin = getOnePermission(
            projectId, repoName, AUTH_BUILTIN_ADMIN,
            ActionCollection.getDefaultAdminBuiltinPermission(repoName)
        )
        val repoUser = getOnePermission(
            projectId,
            repoName,
            AUTH_BUILTIN_USER,
            ActionCollection.getDefaultUserBuiltinPermission(repoName)
        )
        val repoViewer = getOnePermission(
            projectId, repoName, AUTH_BUILTIN_VIEWER,
            ActionCollection.getDefaultViewerBuiltinPermission(repoName)
        )
        val permissions = listOf(repoAdmin, repoUser, repoViewer).map { transferPermission(it) }
        // 过滤非业务权限
        val targetPermissions = mutableListOf<Permission>()
        for (permission in permissions) {
            val actions = permission.actions
            val targetActions = mutableSetOf<String>()
            for (action in actions) {
                if (ActionCollection.isCanwayAction(PermissionAction.valueOf(action))) targetActions.add(action)
            }
            targetPermissions.add(permission.copy(actions = targetActions.map { it }))
        }
        return targetPermissions
    }

    override fun createPermission(request: CreatePermissionRequest): Boolean {
        logger.info("create  permission request : [$request]")
        // todo check request
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
                resourceType = request.resourceType.name,
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

    fun listRepoPermission(request: ListRepoPermissionRequest): List<String> {
        logger.debug("list repo permission  request : [$request] ")
        if (request.repoNames.isNullOrEmpty()) return emptyList()
        val user = userRepository.findFirstByUserId(request.uid) ?: run {
            throw ErrorCodeException(AuthMessageCode.AUTH_USER_NOT_EXIST)
        }
        if (user.admin || !request.appId.isNullOrBlank()) {
            // 查询该项目下的所有仓库并过滤返回
            val repoList = repositoryClient.listRepo(request.projectId).data?.map { it.name } ?: emptyList()
            return filterRepos(repoList, request.repoNames)
        }
        val roles = user.roles

        // check project admin
        if (roles.isNotEmpty() && request.resourceType == ResourceType.PROJECT) {
            return listProjectPermissions(roles, request)
        }

        val reposList = mutableListOf<String>()
        // check repo admin
        if (roles.isNotEmpty() && request.resourceType == ResourceType.REPO) {
            return listRepoPermissions(roles, request, reposList)
        }

        // check repo permission
        with(request) {
            val celeriac = buildCheckActionQuery(projectId, uid, action, request.resourceType, roles, null)
            val query = Query.query(celeriac)
            val result = mongoTemplate.find(query, TPermission::class.java)
            val permissionRepoList = result.stream().flatMap { it.repos.stream() }.collect(Collectors.toList())
            reposList.addAll(permissionRepoList)
            return filterRepos(reposList, request.repoNames)
        }
    }

    override fun registerResource(request: RegisterResourceRequest) {
        return
    }

    // ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
    // 避免service 层逻辑变动影响业务逻辑，完全复制默认service 层所有方法

    private fun originCheckPermission(request: CheckPermissionRequest): Boolean {
        logger.debug("check permission  request : [$request] ")
        val user = userRepository.findFirstByUserId(request.uid) ?: run {
            throw ErrorCodeException(AuthMessageCode.AUTH_USER_NOT_EXIST)
        }

        // check user admin permission
        if (user.admin || !request.appId.isNullOrBlank()) return true

        // check role project admin
        if (checkProjectAdmin(request, user.roles)) return true

        // check role repo admin
        if (checkRepoAdmin(request, user.roles)) return true

        // check repo action action
        return checkRepoAction(request, user.roles)
    }

    private fun checkProjectAdmin(request: CheckPermissionRequest, roles: List<String>): Boolean {
        if (roles.isNotEmpty() && request.projectId != null) {
            roles.forEach {
                val role = roleRepository.findFirstByIdAndProjectIdAndType(it, request.projectId!!, RoleType.PROJECT)
                if (role != null && role.admin) return true
            }
        }
        return false
    }

    private fun checkRepoAdmin(request: CheckPermissionRequest, roles: List<String>): Boolean {
        // check role repo admin
        if (roles.isNotEmpty() && request.projectId != null && request.repoName != null) {
            roles.forEach {
                val rRole = roleRepository.findFirstByIdAndProjectIdAndTypeAndRepoName(
                    it,
                    request.projectId!!,
                    RoleType.REPO,
                    request.repoName!!
                )
                if (rRole != null && rRole.admin) return true
            }
        }
        return false
    }

    private fun checkRepoAction(request: CheckPermissionRequest, roles: List<String>): Boolean {
        with(request) {
            projectId?.let {
                val resultRole = mutableListOf<String>()
                resultRole.addAll(roles)
                request.role?.let { resultRole.add(it) }
                var celeriac = buildCheckActionQuery(
                    projectId!!,
                    uid,
                    PermissionAction.valueOf(action),
                    ResourceType.valueOf(resourceType),
                    resultRole,
                    department
                )

                if (request.resourceType == ResourceType.REPO.name) {
                    celeriac = celeriac.and(TPermission::repos.name).`is`(request.repoName)
                }
                val query = Query.query(celeriac)
                val result = mongoTemplate.count(query, TPermission::class.java)
                if (result != 0L) return true
            }
            return false
        }
    }

    private fun buildCheckActionQuery(
        projectId: String,
        uid: String,
        action: PermissionAction,
        resourceType: ResourceType,
        roles: List<String>,
        department: String?
    ): Criteria {
        val criteria = Criteria()
        var celeriac = criteria.orOperator(
            Criteria.where(TPermission::users.name).`in`(uid),
            Criteria.where(TPermission::roles.name).`in`(roles),
            Criteria.where(TPermission::departments.name).`in`(department)
        )
            .and(TPermission::resourceType.name).`is`(resourceType.toString()).and(TPermission::actions.name)
            .`in`(action.toString())
        if (resourceType != ResourceType.SYSTEM) {
            celeriac = celeriac.and(TPermission::projectId.name).`is`(projectId)
        }
        return celeriac
    }

    private fun checkPermissionExist(pId: String) {
        permissionRepository.findFirstById(pId) ?: run {
            logger.warn("update permission repos [$pId]  not exist.")
            throw ErrorCodeException(AuthMessageCode.AUTH_PERMISSION_NOT_EXIST)
        }
    }

    private fun listProjectPermissions(roles: List<String>, request: ListRepoPermissionRequest): List<String> {
        roles.forEach { role ->
            val tRole = roleRepository.findFirstByIdAndProjectIdAndType(role, request.projectId, RoleType.PROJECT)
            if (tRole != null && tRole.admin) {
                val repoList = repositoryClient.listRepo(request.projectId).data?.map { it.name } ?: emptyList()
                return filterRepos(repoList, request.repoNames)
            }
        }
        return emptyList()
    }

    private fun listRepoPermissions(
        roles: List<String>,
        request: ListRepoPermissionRequest,
        reposList: MutableList<String>
    ): List<String> {
        roles.forEach { role ->
            // check project admin first
            val pRole = roleRepository.findFirstByIdAndProjectIdAndType(role, request.projectId, RoleType.PROJECT)
            if (pRole != null && pRole.admin) {
                val repoList = repositoryClient.listRepo(request.projectId).data?.map { it.name } ?: emptyList()
                return filterRepos(repoList, request.repoNames)
            }
            // check repo admin then
            val rRole = roleRepository.findFirstByIdAndProjectIdAndType(
                role,
                request.projectId,
                RoleType.REPO
            )
            if (rRole != null && rRole.admin) reposList.add(rRole.repoName!!)
        }
        return emptyList()
    }

    private fun getOnePermission(
        projectId: String,
        repoName: String,
        permName: String,
        actions: List<PermissionAction>
    ): TPermission {
        logger.info("start check permission : $projectId, $repoName, $permName , $actions")
        return findPermission(projectId, repoName, permName) ?: run {
            val request = TPermission(
                projectId = projectId,
                repos = listOf(repoName),
                permName = permName,
                actions = actions.map { it.name },
                resourceType = ResourceType.REPO.name,
                createAt = LocalDateTime.now(),
                updateAt = LocalDateTime.now(),
                createBy = AUTH_ADMIN,
                updatedBy = AUTH_ADMIN
            )
            if (logger.isDebugEnabled) {
                logger.debug("create permission request [$request]")
            }
            try {
                permissionRepository.insert(request)
            } catch (exception: DuplicateKeyException) {
                logger.error("insert permission [$request] error: [${exception.message}]")
                findPermission(projectId, repoName, permName)!!
            }
        }
    }

    private fun findPermission(projectId: String, repoName: String, permName: String): TPermission? {
        logger.info("find permission : $projectId, $repoName, $permName ")
        return permissionRepository.findOneByProjectIdAndReposContainsAndPermNameAndResourceType(
            projectId,
            repoName,
            permName,
            ResourceType.REPO
        )
    }

    // ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

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
        val actions = request.actions
        val targetActions = mutableSetOf<PermissionAction>()
        for (action in actions) {
            if (ActionCollection.isCanwayAction(action)) {
                targetActions.addAll(ActionCollection.getActionsByCanway(action))
            }
            targetActions.add(action)
        }
        val targetRequest = request.copy(
            actions = targetActions.map { it }
        )
        return super.updatePermissionAction(targetRequest)
    }

    private fun checkUserHasProjectPermission(operator: String): Boolean {
        val canwayPermissionResponse = getCanwayPermissionInstance(operator)
        return checkInstance(canwayPermissionResponse)
    }

    /**
     * 查询系统级创建
     */
    private fun getCanwayPermissionInstance(operator: String):
        CanwayPermissionResponse? {
            val canwayCheckPermissionRequest = CanwayPermissionRequest(
                userId = operator,
                belongCode = "system",
                belongInstance = "bk_ci",
                resourcesActions = setOf(
                    CanwayPermissionRequest.CanwayAction(
                        actionCode = CanwayPermissionType.CREATE,
                        resourceCode = "project",
                        resourceInstance = setOf(
                            CanwayPermissionRequest.CanwayAction.CanwayInstance(
                                resourceCode = "project"
                            )
                        )
                    )
                )
            ).toJsonString()
            val ciAddResourceUrl = getRequestUrl(ciCheckPermissionApi)
            val responseContent = CanwayHttpUtils.doPost(ciAddResourceUrl, canwayCheckPermissionRequest).content

            return responseContent.readJsonString<CanwayResponse<CanwayPermissionResponse>>().data
        }

    private fun checkInstance(canwayPermission: CanwayPermissionResponse?): Boolean {
        canwayPermission?.let {
            return matchInstance(it.instanceCodes.first().resourceInstance)
        }
        return false
    }

    private fun matchInstance(instances: Set<String>?): Boolean {
        instances?.let {
            if (it.contains("*") || it.contains("bk_ci")) return true
        }
        return false
    }

    private fun checkPermissionById(permissionId: String): Boolean {
        val userId = bkUserService.getBkUser()
        val tPermission = permissionRepository.findFirstById(permissionId)!!
        logger.info("CanwayPermissionService found tPermission: $tPermission")
        val checkPermissionRequest = CheckPermissionRequest(
            uid = userId,
            resourceType = ResourceType.REPO.name,
            action = PermissionAction.REPO_MANAGE.name,
            projectId = tPermission.projectId,
            repoName = tPermission.repos.first()
        )
        return checkPermission(checkPermissionRequest)
    }

    private fun canwayCheckPermission(request: CheckPermissionRequest): CanwayPermissionResult {
        val uid = request.uid
        val projectId = request.projectId
            ?: throw(ErrorCodeException(CommonMessageCode.PARAMETER_MISSING, "`projectId` is must not be null"))
        val repoName = request.repoName

        val resourceType = ResourceType.valueOf(request.resourceType)
        val action = PermissionAction.valueOf(request.action)

        val tPermissions = permissionRepository.findByProjectIdAndReposContainsAndResourceTypeAndActionsContains(
            projectId, repoName, resourceType, action
        )

        if (tPermissions != null) {
            for (tPermission in tPermissions) {
                val canwayPermissionResult = canwayCheckTPermission(uid, tPermission)
                if (canwayPermissionResult.hasPermission) return canwayPermissionResult
            }
        }
        return CanwayPermissionResult(false, null, null)
    }

    /**
     * 用户在部门、用户组、授权用户中任一返回true
     */
    private fun canwayCheckTPermission(uid: String, tPermission: TPermission): CanwayPermissionResult {
        val departments = tPermission.departments
        val departmentResult = checkDepartment(uid, departments)
        val roles = tPermission.roles
        val roleResult = checkGroup(uid, roles)
        val userResult = tPermission.users.contains(uid)
        return CanwayPermissionResult(
            (departmentResult.hasPermission || roleResult.hasPermission || userResult),
            roleResult.role,
            departmentResult.department
        )
    }

    /**
     * 检查用户是否在被授权的用户组内
     */
    private fun checkGroup(uid: String, roles: List<String>): CanwayPermissionRole {
        for (role in roles) {
            val tRole = roleRepository.findFirstById(role)
            tRole?.let {
                getUsersByGroupId(uid, tRole.roleId)?.let {
                    if (it.contains(uid)) {
                        return CanwayPermissionRole(true, role)
                    }
                }
                return CanwayPermissionRole(true, tRole.roleId)
            }
        }
        return CanwayPermissionRole(false, null)
    }

    /**
     * 检查用户是否在被授权的部门内
     */
    private fun checkDepartment(uid: String, departments: List<String>): CanwayPermissionDepartment {
        for (department in departments) {
            departmentService.getUsersByDepartmentId(uid, department.toInt())?.let {
                for (user in it) {
                    if (user.username == uid) return CanwayPermissionDepartment(true, department)
                }
            }
        }
        return CanwayPermissionDepartment(false, null)
    }

    /**
     * 查询出用户组内所有成员
     * [uid] 查询人
     * [groupId] 用户组id
     */
    private fun getUsersByGroupId(uid: String, groupId: String): List<String>? {
        val uri = String.format(getUsersByGroupIdApi, groupId, uid)
        val requestUrl = getRequestUrl(uri)
        val responseContent = CanwayHttpUtils.doGet(requestUrl).content
        return responseContent.readJsonString<CanwayResponse<List<String>>>().data
    }

    private fun getRequestUrl(uri: String): String {
        val devopsHost = devopsConf.devopsHost
        return "${devopsHost.removeSuffix("/")}$ciPermission$ciApi$uri"
    }

    companion object {
        val logger: Logger = LoggerFactory.getLogger(CanwayPermissionServiceImpl::class.java)
        const val getUsersByGroupIdApi = "$ciPermission$ciApi/service/organization/%s?userId=%s"
        const val ciCheckPermissionApi = "/service/resource_instance/query"
    }
}
