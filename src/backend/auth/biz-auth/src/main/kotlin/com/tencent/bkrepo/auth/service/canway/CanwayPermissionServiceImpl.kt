package com.tencent.bkrepo.auth.service.canway

import com.tencent.bkrepo.auth.constant.AUTH_BUILTIN_ADMIN
import com.tencent.bkrepo.auth.constant.AUTH_BUILTIN_USER
import com.tencent.bkrepo.auth.constant.AUTH_BUILTIN_VIEWER
import com.tencent.bkrepo.auth.message.AuthMessageCode
import com.tencent.bkrepo.auth.model.TPermission
import com.tencent.bkrepo.auth.pojo.enums.PermissionAction
import com.tencent.bkrepo.auth.pojo.enums.ResourceType
import com.tencent.bkrepo.auth.pojo.permission.CheckPermissionRequest
import com.tencent.bkrepo.auth.pojo.permission.Permission
import com.tencent.bkrepo.auth.pojo.permission.UpdatePermissionActionRequest
import com.tencent.bkrepo.auth.pojo.permission.UpdatePermissionDepartmentRequest
import com.tencent.bkrepo.auth.pojo.permission.UpdatePermissionUserRequest
import com.tencent.bkrepo.auth.pojo.permission.UpdatePermissionRoleRequest
import com.tencent.bkrepo.auth.repository.PermissionRepository
import com.tencent.bkrepo.auth.repository.RoleRepository
import com.tencent.bkrepo.auth.repository.UserRepository
import com.tencent.bkrepo.auth.service.DepartmentService
import com.tencent.bkrepo.auth.service.canway.bk.BkUserService
import com.tencent.bkrepo.auth.service.canway.conf.CanwayAuthConf
import com.tencent.bkrepo.auth.service.canway.http.CanwayHttpUtils
import com.tencent.bkrepo.auth.service.canway.pojo.ActionCollection
import com.tencent.bkrepo.auth.service.canway.pojo.CanwayPermissionRequest
import com.tencent.bkrepo.auth.service.canway.pojo.CanwayPermissionResponse
import com.tencent.bkrepo.auth.service.canway.pojo.CanwayResponse
import com.tencent.bkrepo.auth.service.canway.pojo.CanwayPermissionResult
import com.tencent.bkrepo.auth.service.canway.pojo.CanwayPermissionRole
import com.tencent.bkrepo.auth.service.canway.pojo.CanwayPermissionDepartment
import com.tencent.bkrepo.auth.service.local.PermissionServiceImpl
import com.tencent.bkrepo.common.api.exception.ErrorCodeException
import com.tencent.bkrepo.common.api.message.CommonMessageCode
import com.tencent.bkrepo.common.api.util.readJsonString
import com.tencent.bkrepo.common.api.util.toJsonString
import com.tencent.bkrepo.common.service.util.HttpContextHolder
import com.tencent.bkrepo.repository.api.RepositoryClient
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.data.mongodb.core.MongoTemplate

class CanwayPermissionServiceImpl(
    private val userRepository: UserRepository,
    private val roleRepository: RoleRepository,
    permissionRepository: PermissionRepository,
    mongoTemplate: MongoTemplate,
    repositoryClient: RepositoryClient,
    private val canwayAuthConf: CanwayAuthConf,
    private val departmentService: DepartmentService,
    private val bkUserService: BkUserService
) : PermissionServiceImpl(userRepository, roleRepository, permissionRepository, mongoTemplate, repositoryClient) {

    override fun checkPermission(request: CheckPermissionRequest): Boolean {
        logger.info("check permission  request : [$request] ")
        // 校验用户是否属于对应部门、用户组和已添加用户
        if(isBkrepoAdmin(request)) return true
        if (checkUserHasProjectPermission(request.uid)) return true
        val canwayPermissionResult = canwayCheckPermission(request)
        if (!canwayPermissionResult.hasPermission) return false
        val action = request.action
        if (ActionCollection.isCanwayAction(action)) {
            val actions = ActionCollection.getActionsByCanway(action)
            for (ac in actions) {
                val tempRequest = request.copy(
                    action = ac,
                    role = canwayPermissionResult.roles,
                    department = canwayPermissionResult.departments
                )
                val result = super.checkPermission(tempRequest)
                if (!result) return false
            }
            return true
        }
        return super.checkPermission(request)
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
            if (!checkPermissionById(request.permissionId)) throw ErrorCodeException(CommonMessageCode.PERMISSION_DENIED)
        }
        return super.updatePermissionDepartment(request)
    }

    override fun updatePermissionUser(request: UpdatePermissionUserRequest): Boolean {
        val webRequest = HttpContextHolder.getRequest()
        val api = webRequest.requestURI.removePrefix("/").removePrefix("web/")
        if (api.startsWith("api", ignoreCase = true)) {
            if (!checkPermissionById(request.permissionId)) throw ErrorCodeException(CommonMessageCode.PERMISSION_DENIED)
        }
        return super.updatePermissionUser(request)
    }

    override fun updatePermissionRole(request: UpdatePermissionRoleRequest): Boolean {
        val webRequest = HttpContextHolder.getRequest()
        val api = webRequest.requestURI.removePrefix("/").removePrefix("web/")
        if (api.startsWith("api", ignoreCase = true)) {
            if (!checkPermissionById(request.permissionId)) throw ErrorCodeException(CommonMessageCode.PERMISSION_DENIED)
        }
        return super.updatePermissionRole(request)
    }

    override fun updatePermissionAction(request: UpdatePermissionActionRequest): Boolean {
        val webRequest = HttpContextHolder.getRequest()
        val api = webRequest.requestURI.removePrefix("/").removePrefix("web/")
        if (api.startsWith("api", ignoreCase = true)) {
            if (!checkPermissionById(request.permissionId)) throw ErrorCodeException(CommonMessageCode.PERMISSION_DENIED)
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

    override fun listBuiltinPermission(projectId: String, repoName: String): List<Permission> {
        logger.debug("list  builtin permission  projectId: [$projectId], repoName: [$repoName]")
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
            val targetActions = mutableSetOf<PermissionAction>()
            for (action in actions) {
                if (ActionCollection.isCanwayAction(action)) targetActions.add(action)
            }
            targetPermissions.add(permission.copy(actions = targetActions.map { it }))
        }
        return targetPermissions
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
                        actionCode = "create",
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
        val checkPermissionRequest = CheckPermissionRequest(
            uid = userId,
            resourceType = ResourceType.REPO,
            action = PermissionAction.REPO_MANAGE,
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

        val resourceType = request.resourceType
        val action = request.action

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
        return CanwayPermissionResult((departmentResult.hasPermission || roleResult.hasPermission || userResult), roleResult.role, departmentResult.department)
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
        val devopsHost = canwayAuthConf.devopsHost ?: throw ErrorCodeException(CommonMessageCode.PARAMETER_MISSING)
        return "${devopsHost.removeSuffix("/")}$ci$ciApi$uri"
    }

    companion object {
        val logger: Logger = LoggerFactory.getLogger(CanwayPermissionServiceImpl::class.java)
        const val getUsersByGroupIdApi = "$ci$ciApi/service/organization/%s?userId=%s"
        const val ciCheckPermissionApi = "/service/resource_instance/query"
    }
}
