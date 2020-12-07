package com.tencent.bkrepo.auth.service.canway

import com.tencent.bkrepo.auth.pojo.enums.PermissionAction
import com.tencent.bkrepo.auth.pojo.permission.CheckPermissionRequest
import com.tencent.bkrepo.auth.pojo.permission.UpdatePermissionActionRequest
import com.tencent.bkrepo.auth.pojo.permission.UpdatePermissionDepartmentRequest
import com.tencent.bkrepo.auth.pojo.permission.UpdatePermissionRoleRequest
import com.tencent.bkrepo.auth.pojo.permission.UpdatePermissionUserRequest
import com.tencent.bkrepo.auth.pojo.permission.Permission
import com.tencent.bkrepo.auth.repository.PermissionRepository
import com.tencent.bkrepo.auth.repository.RoleRepository
import com.tencent.bkrepo.auth.repository.UserRepository
import com.tencent.bkrepo.auth.service.canway.bk.BkUserService
import com.tencent.bkrepo.auth.service.canway.conf.CanwayAuthConf
import com.tencent.bkrepo.auth.service.canway.pojo.CanwayResponse
import com.tencent.bkrepo.auth.service.canway.pojo.UserResourceAuthResponse
import com.tencent.bkrepo.auth.service.canway.pojo.UserResourcesAuthRequest
import com.tencent.bkrepo.auth.service.canway.pojo.CanwayRole
import com.tencent.bkrepo.auth.service.canway.pojo.CanwayRoleResourceRequest
import com.tencent.bkrepo.auth.service.canway.pojo.CanwayFunction
import com.tencent.bkrepo.auth.service.canway.pojo.CanwayRolePermissionRequest
import com.tencent.bkrepo.auth.service.canway.pojo.CanwayResourceDetail
import com.tencent.bkrepo.auth.service.canway.pojo.CanwayRoleRequest
import com.tencent.bkrepo.auth.service.canway.enum.Role
import com.tencent.bkrepo.auth.service.local.PermissionServiceImpl
import com.tencent.bkrepo.auth.util.HttpUtils
import com.tencent.bkrepo.common.api.exception.ErrorCodeException
import com.tencent.bkrepo.common.api.message.CommonMessageCode
import com.tencent.bkrepo.common.api.util.readJsonString
import com.tencent.bkrepo.common.api.util.toJsonString
import com.tencent.bkrepo.common.service.util.HttpContextHolder
import com.tencent.bkrepo.repository.api.RepositoryClient
import okhttp3.MediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.data.mongodb.core.MongoTemplate
import java.util.concurrent.TimeUnit

class CanwayPermissionServiceImpl(
    userRepository: UserRepository,
    roleRepository: RoleRepository,
    permissionRepository: PermissionRepository,
    mongoTemplate: MongoTemplate,
    repositoryClient: RepositoryClient,
    private val canwayAuthConf: CanwayAuthConf,
    private val bkUserService: BkUserService
) : PermissionServiceImpl(userRepository, roleRepository, permissionRepository, mongoTemplate, repositoryClient) {

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(3L, TimeUnit.SECONDS)
        .readTimeout(5L, TimeUnit.SECONDS)
        .writeTimeout(5L, TimeUnit.SECONDS)
        .build()

    override fun checkPermission(request: CheckPermissionRequest): Boolean {
        logger.info("check permission  request : [$request] ")
        // canway 权限中心 权限校验
        return checkCanwayPermission(request)
    }

    override fun updatePermissionAction(request: UpdatePermissionActionRequest): Boolean {
        // 查询角色
        val role = permissionRepository.findFirstById(request.permissionId)
            ?: throw ErrorCodeException(CommonMessageCode.PARAMETER_INVALID, "${request.permissionId} can not found")
        val projectId = role.projectId
        val canwayRole = getCanwayRoleByRoleName(projectId!!, role.permName)
        addActionToRole(canwayRole, request.actions)
        return super.updatePermissionAction(request)
    }

    override fun updatePermissionDepartment(request: UpdatePermissionDepartmentRequest): Boolean {
        val role = permissionRepository.findFirstById(request.permissionId)
            ?: throw ErrorCodeException(CommonMessageCode.PARAMETER_INVALID, "${request.permissionId} can not found")
        val projectId = role.projectId
        val canwayRole = getCanwayRoleByRoleName(projectId!!, role.permName)
        updataCanwayResource(canwayRole, request.departmentId)
        return super.updatePermissionDepartment(request)
    }

    override fun updatePermissionRole(request: UpdatePermissionRoleRequest): Boolean {
        val role = permissionRepository.findFirstById(request.permissionId)
            ?: throw ErrorCodeException(CommonMessageCode.PARAMETER_INVALID, "${request.permissionId} can not found")
        val projectId = role.projectId
        val canwayRole = getCanwayRoleByRoleName(projectId!!, role.permName)
        updataCanwayResource(canwayRole, request.rId)
        return super.updatePermissionRole(request)
    }

    override fun updatePermissionUser(request: UpdatePermissionUserRequest): Boolean {
        //
        val role = permissionRepository.findFirstById(request.permissionId)
            ?: throw ErrorCodeException(CommonMessageCode.PARAMETER_INVALID, "${request.permissionId} can not found")
        val projectId = role.projectId
        val canwayRole = getCanwayRoleByRoleName(projectId!!, role.permName)
        updataCanwayResource(canwayRole, request.userId)
        return super.updatePermissionUser(request)
    }

    /**
     * canway 权限中心 更新角色绑定的用户
     * [canwayRole]  canway 权限中心 角色
     * [userIds]     用户列表（uid）
     */
    private fun updataCanwayResource(canwayRole: CanwayRole, userIds: List<String>) {
        val userId = bkUserService.getBkUser()
        val uri = String.format(updateRoleResourceApi, canwayRole.id, userId)
        val devopsHost = canwayAuthConf.devopsHost ?: throw ErrorCodeException(CommonMessageCode.PARAMETER_MISSING)
        val requestUrl = "${devopsHost.removeSuffix("/")}$uri"
        val canwayRoleResourceRequest = CanwayRoleResourceRequest(
            groupId = canwayRole.id,
            memberList = userIds.map { userIdToMember(it) }
        )

        val requestBody = RequestBody.create(mediaType, canwayRoleResourceRequest.toJsonString())
        val request = Request.Builder()
            .url(requestUrl)
            .post(requestBody).build()
        HttpUtils.doRequest(okHttpClient, request, 3, mutableSetOf(200))
    }

    /**
     *
     */
    private fun userIdToMember(userId: String): CanwayRoleResourceRequest.Member {
        return CanwayRoleResourceRequest.Member(userId, USER_TYPE)
    }

    /**
     * canway 权限中心 向角色添加动作
     * [canwayRole]  canway 权限中心 角色
     * [actions] 动作ID列表
     */
    private fun addActionToRole(canwayRole: CanwayRole, actions: List<PermissionAction>) {
        val userId = bkUserService.getBkUser()
        val uri = String.format(updataRoleActionApi, canwayRole.id, userId)
        val devopsHost = canwayAuthConf.devopsHost ?: throw ErrorCodeException(CommonMessageCode.PARAMETER_MISSING)
        val requestUrl = "${devopsHost.removeSuffix("/")}$uri"
        val canwayActions = actions.map { transferAction(it) }
        val canwayRolePermissionRequest = CanwayRolePermissionRequest(
            code = ciResourceCode,
            name = "",
            functionList = canwayActions
        )
        val requestBody = RequestBody.create(mediaType, canwayRolePermissionRequest.toJsonString())
        val request = Request.Builder()
            .url(requestUrl)
            .post(requestBody)
            .build()
        HttpUtils.doRequest(okHttpClient, request, 3, mutableSetOf(200))
    }

    private fun transferAction(action: PermissionAction): CanwayFunction {
        return CanwayFunction(getCanwayFunctionId(action))
    }

    fun getCanwayFunctionId(action: PermissionAction): String {
        val userId = bkUserService.getBkUser()
        val uri = String.format(resourceDetail, userId)
        val devopsHost = canwayAuthConf.devopsHost ?: throw ErrorCodeException(CommonMessageCode.PARAMETER_MISSING)
        val requestUrl = "${devopsHost.removeSuffix("/")}$uri"
        val request = Request.Builder()
            .url(requestUrl)
            .build()
        val responseContent = HttpUtils.doRequest(okHttpClient, request, 3, mutableSetOf(200)).content
        val canwayResourceDetail = responseContent.readJsonString<CanwayResponse<CanwayResourceDetail>>().data
        val functionList = canwayResourceDetail!!.functionList
        for (function in functionList) {
            if (function.action.code.equals(action.name, ignoreCase = true)) return function.id
        }
        throw ErrorCodeException(CommonMessageCode.PARAMETER_MISSING, "can not find action:${action.name} in ci")
    }

    /**
     * 根据角色名称查询canway权限中心的角色
     * [roleName]  Role.value
     */
    private fun getCanwayRoleByRoleName(projectId: String, roleName: String): CanwayRole {
        val canwayRoleName = transferRoleName(roleName)
        val canwayRoleList = listCanwayRoleByProject(projectId)
            ?: throw ErrorCodeException(CommonMessageCode.SERVICE_CALL_ERROR, "can not load canway role")
        for (canwayRole in canwayRoleList) {
            if (canwayRole.name == canwayRoleName) {
                return canwayRole
            }
        }
        throw ErrorCodeException(CommonMessageCode.SERVICE_CALL_ERROR, "can not load canway role: $roleName")
    }

    private fun transferRoleName(roleName: String): String {
        return when (roleName) {
            Role.ADMIN.value -> REPO_ADMIN
            Role.USER.value -> REPO_USER
            Role.VIEWER.value -> REPO_VIEWER
            else -> throw ErrorCodeException(CommonMessageCode.PARAMETER_INVALID)
        }
    }

    override fun listBuiltinPermission(projectId: String, repoName: String): List<Permission> {
        // 检查ci权限中心是否有对应初始角色。
        checkCanwayRoleExist(projectId)
        return super.listBuiltinPermission(projectId, repoName)
    }

    /**
     * 检查预先定义的角色在canway权限中心是否存在，不存在则删除
     */
    fun checkCanwayRoleExist(projectId: String) {
        // 按名称检查
        val roles = mutableListOf(Role.ADMIN, Role.USER, Role.VIEWER)
        val existsRole = mutableListOf<Role>()
        val canwayRoleList = listCanwayRoleByProject(projectId) ?: createCiRole(projectId, roles)
        for (canwayRole in canwayRoleList) {
            if (roles.customContains(canwayRole.name)) existsRole.customAdd(roles, canwayRole.name)
        }
        roles.removeAll(existsRole)
        // 如果被删掉的权限，再次添加
        createCiRole(projectId, roles)
    }

    private fun List<Role>.customContains(value: String): Boolean {
        val rolesNickList = mutableListOf<String>()
        for (role in this) {
            rolesNickList.add(role.nickName())
        }
        return rolesNickList.contains(value)
    }

    private fun MutableList<Role>.customAdd(roles: List<Role>, value: String): MutableList<Role> {
        for (role in roles) {
            if (role.nickName() == value) this.add(role)
        }
        return this
    }

    /**
     * 在canway权限中心 创建角色
     */
    private fun createCiRole(projectId: String, roleList: List<Role>): List<CanwayRole> {
        val userId = bkUserService.getBkUser()
        for (role in roleList) {
            try {
                val canwayRoleRequestBody = CanwayRoleRequest(
                    name = role.nickName(),
                    parentId = "",
                    service = ciBelongCode,
                    belongInstance = projectId,
                    belongCode = ciBelongCode,
                    description = "create by bkrepo"
                )
                val requestBody = RequestBody.create(mediaType, canwayRoleRequestBody.toJsonString())
                val uri = String.format(addRoleApi, userId)
                val devopsHost = canwayAuthConf.devopsHost
                    ?: throw ErrorCodeException(CommonMessageCode.PARAMETER_MISSING)
                val requestUrl = "${devopsHost.removeSuffix("/")}$uri"
                val request = Request.Builder()
                    .url(requestUrl)
                    .post(requestBody)
                    .build()
                // 创建角色
                HttpUtils.doRequest(OkHttpClient(), request, 3, mutableSetOf(200))
                // 更新角色权限
                val canwayRole = getCanwayRoleByRoleName(projectId, role.value)
                addActionToRole(canwayRole, getDefaultActionsByRole(role))
            } catch (exception: Exception) {
                logger.error("add role ${role.nickName()} by $userId failed, ${exception.message}")
                throw exception
            }
        }
        return listCanwayRoleByProject(projectId)
            ?: throw ErrorCodeException(CommonMessageCode.SERVICE_CALL_ERROR, "add role by $userId failed")
    }

    private fun getDefaultActionsByRole(role: Role): List<PermissionAction> {
        return when (role) {
            Role.ADMIN -> ADMIN
            Role.USER -> USER
            Role.VIEWER -> VIEWER
        }
    }

    /**
     * 查询项目下 canway 的角色列表
     */
    private fun listCanwayRoleByProject(projectId: String): List<CanwayRole>? {
        val devopsHost = canwayAuthConf.devopsHost ?: throw ErrorCodeException(CommonMessageCode.PARAMETER_MISSING)
        val uri = String.format(roleListApi, ciBelongCode, projectId)
        val requestUrl = "${devopsHost.removeSuffix("/")}$uri"

        val request = Request.Builder().url(requestUrl).build()
        val responseContent = HttpUtils.doRequest(OkHttpClient(), request, 3, mutableSetOf(200)).content
        return responseContent.readJsonString<CanwayResponse<List<CanwayRole>>>().data
    }

    /**
     * 获取canway 权限中心 租户
     */
    private fun getTenantId(): String {
        val cookies = HttpContextHolder.getRequest().cookies
            ?: throw ErrorCodeException(CommonMessageCode.HEADER_MISSING)
        var tenant: String? = null
        for (cookie in cookies) {
            if (cookie.name == ciTenant) tenant = cookie.value
        }
        if (tenant == null) throw ErrorCodeException(CommonMessageCode.HEADER_MISSING)
        return tenant
    }

    /**
     * 在canway 权限中心鉴权
     */
    private fun checkCanwayPermission(request: CheckPermissionRequest): Boolean {
        val canwayRequest = createCanwayPermissionRequest(request)

        val responseContent = HttpUtils.doRequest(OkHttpClient(), canwayRequest, 3, mutableSetOf(200)).content

        val userResources = responseContent.readJsonString<CanwayResponse<UserResourceAuthResponse>>()
            .data ?: throw ErrorCodeException(CommonMessageCode.RESOURCE_NOT_FOUND)

        val instanceList = userResources.instanceCodes ?: return false

        return match(request, instanceList)
    }

    private fun createCanwayPermissionRequest(request: CheckPermissionRequest): Request {
        val userId = request.uid
        val projectId = request.projectId
        val repoName = request.repoName
        val actionCode = request.action.id()
        val devopsHost = canwayAuthConf.devopsHost ?: throw ErrorCodeException(CommonMessageCode.PARAMETER_MISSING)
        val body = UserResourcesAuthRequest(
            userId = userId,
            belongCode = "project",
            belongInstance = projectId,
            resourcesActions = mutableListOf(
                UserResourcesAuthRequest.ResourcesAction(
                    actionCode = actionCode,
                    resourceCode = ciResourceCode,
                    resourceInstance = mutableListOf(
                        UserResourcesAuthRequest.ResourceInstance(
                            resourceCode = ciResourceCode,
                            instanceCode = repoName ?: ""
                        )
                    )
                )
            )
        )

        val requestBody = RequestBody.create(mediaType, body.toJsonString())
        val requestUrl = "${devopsHost.removeSuffix("/")}$checkPermissionApi"
        return Request.Builder()
            .url(requestUrl)
            .post(requestBody)
            .build()
    }

    /**
     * 检查 用户在canway权限中心是否有该-实例-的权限
     */
    private fun match(request: CheckPermissionRequest, instanceList: List<UserResourceAuthResponse.ResourcesAction>): Boolean {
        val action = request.action
        if (action == PermissionAction.MANAGE) {
            instanceList.first().resourceInstance?.let {
                if (it == mutableSetOf("*")) return true
            }
            return false
        }
        val repoName = request.repoName

        for (resourceAction in instanceList) {
            if (resourceAction.resourceInstance != null && resourceAction.resourceInstance.contains(repoName)) return true
        }
        return false
    }

    companion object {
        val logger: Logger = LoggerFactory.getLogger(CanwayPermissionServiceImpl::class.java)
        val mediaType = MediaType.parse("application/json; charset=utf-8")
        const val checkPermissionApi = "$ci$ciApi/service/resource_instance/query"
        // 项目下角色列表
        const val roleListApi = "$ci$ciApi/service/role/%s/%s"
        const val addRoleApi = "$ci$ciApi/service/role?userId=%s"
        const val updateRoleResourceApi = "$ci$ciApi/service/role/associateUser?userId=%s"
        const val updataRoleActionApi = "$ci$ciApi/service/role/%s/updateResource?userId=%s"
        const val resourceDetail = "$ci$ciApi/service/resources/$ciResourceCode?userId=%s"
        // 默认初始权限模板
        val ADMIN = listOf(PermissionAction.MANAGE)
        val USER = listOf(PermissionAction.WRITE, PermissionAction.DELETE, PermissionAction.UPDATE, PermissionAction.READ)
        val VIEWER = listOf(PermissionAction.READ)
    }
}
