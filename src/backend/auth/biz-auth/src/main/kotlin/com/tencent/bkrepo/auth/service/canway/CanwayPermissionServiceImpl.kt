package com.tencent.bkrepo.auth.service.canway

import com.tencent.bkrepo.auth.constant.AUTH_BUILTIN_ADMIN
import com.tencent.bkrepo.auth.constant.AUTH_BUILTIN_USER
import com.tencent.bkrepo.auth.constant.AUTH_BUILTIN_VIEWER
import com.tencent.bkrepo.auth.model.TPermission
import com.tencent.bkrepo.auth.pojo.enums.PermissionAction
import com.tencent.bkrepo.auth.pojo.permission.CheckPermissionRequest
import com.tencent.bkrepo.auth.pojo.permission.Permission
import com.tencent.bkrepo.auth.pojo.permission.UpdatePermissionActionRequest
import com.tencent.bkrepo.auth.repository.PermissionRepository
import com.tencent.bkrepo.auth.repository.RoleRepository
import com.tencent.bkrepo.auth.repository.UserRepository
import com.tencent.bkrepo.auth.service.DepartmentService
import com.tencent.bkrepo.auth.service.canway.conf.CanwayAuthConf
import com.tencent.bkrepo.auth.service.canway.http.CanwayHttpUtils
import com.tencent.bkrepo.auth.service.canway.pojo.ActionCollection
import com.tencent.bkrepo.auth.service.canway.pojo.CanwayResponse
import com.tencent.bkrepo.auth.service.canway.pojo.bk.BkDepartmentUser
import com.tencent.bkrepo.auth.service.local.PermissionServiceImpl
import com.tencent.bkrepo.common.api.exception.ErrorCodeException
import com.tencent.bkrepo.common.api.message.CommonMessageCode
import com.tencent.bkrepo.common.api.util.readJsonString
import com.tencent.bkrepo.repository.api.RepositoryClient
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.data.mongodb.core.MongoTemplate

class CanwayPermissionServiceImpl(
    userRepository: UserRepository,
    roleRepository: RoleRepository,
    permissionRepository: PermissionRepository,
    mongoTemplate: MongoTemplate,
    repositoryClient: RepositoryClient,
    private val canwayAuthConf: CanwayAuthConf,
    private val departmentService: DepartmentService
) : PermissionServiceImpl(userRepository, roleRepository, permissionRepository, mongoTemplate, repositoryClient) {

    override fun checkPermission(request: CheckPermissionRequest): Boolean {
        logger.info("check permission  request : [$request] ")
        // 校验用户是否属于对应部门和用户组
        canwayCheckPermission(request)
        val action = request.action
        if (ActionCollection.isCanwayAction(action)) {
            val actions = ActionCollection.getActionsByCanway(action)
            for (ac in actions) {
                val tempRequest = request.copy(
                    action = ac
                )
                val result = super.checkPermission(tempRequest)
                if (!result) return false
            }
            return true
        }
        return super.checkPermission(request)
    }

    override fun updatePermissionAction(request: UpdatePermissionActionRequest): Boolean {
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
            getDefaultAdminBuiltinPermission(repoName)
        )
        val repoUser = getOnePermission(
            projectId,
            repoName,
            AUTH_BUILTIN_USER,
            getDefaultUserBuiltinPermission(repoName)
        )
        val repoViewer = getOnePermission(projectId, repoName, AUTH_BUILTIN_VIEWER, getDefaultViewerBuiltinPermission(repoName))
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

    private fun getDefaultAdminBuiltinPermission(repoName: String): List<PermissionAction> {
        return when (repoName) {
            "custom" -> listOf(
                PermissionAction.MANAGE,
                PermissionAction.READ,
                PermissionAction.WRITE,
                PermissionAction.UPDATE,
                PermissionAction.DELETE,
                PermissionAction.REPO_MANAGE,
                PermissionAction.FOLDER_MANAGE,
                PermissionAction.ARTIFACT_COPY,
                PermissionAction.ARTIFACT_RENAME,
                PermissionAction.ARTIFACT_MOVE,
                PermissionAction.ARTIFACT_SHARE,
                PermissionAction.ARTIFACT_DOWNLOAD,
                PermissionAction.ARTIFACT_READWRITE,
                PermissionAction.ARTIFACT_READ
            )
            "pipeline" -> listOf(
                PermissionAction.MANAGE,
                PermissionAction.READ,
                PermissionAction.UPDATE,
                PermissionAction.WRITE,
                PermissionAction.REPO_MANAGE,
                PermissionAction.ARTIFACT_SHARE,
                PermissionAction.ARTIFACT_DOWNLOAD,
                PermissionAction.ARTIFACT_READ
            )
            else -> listOf(
                PermissionAction.MANAGE,
                PermissionAction.READ,
                PermissionAction.WRITE,
                PermissionAction.UPDATE,
                PermissionAction.DELETE,
                PermissionAction.REPO_MANAGE,
                PermissionAction.ARTIFACT_UPDATE,
                PermissionAction.ARTIFACT_DOWNLOAD,
                PermissionAction.ARTIFACT_READWRITE,
                PermissionAction.ARTIFACT_READ,
                PermissionAction.ARTIFACT_DELETE
            )
        }
    }

    private fun getDefaultUserBuiltinPermission(repoName: String): List<PermissionAction> {
        return when (repoName) {
            "custom" -> listOf(
                PermissionAction.READ,
                PermissionAction.WRITE,
                PermissionAction.UPDATE,
                PermissionAction.DELETE,
                PermissionAction.FOLDER_MANAGE,
                PermissionAction.ARTIFACT_COPY,
                PermissionAction.ARTIFACT_RENAME,
                PermissionAction.ARTIFACT_MOVE,
                PermissionAction.ARTIFACT_SHARE,
                PermissionAction.ARTIFACT_DOWNLOAD,
                PermissionAction.ARTIFACT_READWRITE,
                PermissionAction.ARTIFACT_READ
            )
            "pipeline" -> listOf(
                PermissionAction.READ,
                PermissionAction.UPDATE,
                PermissionAction.WRITE,
                PermissionAction.ARTIFACT_SHARE,
                PermissionAction.ARTIFACT_DOWNLOAD,
                PermissionAction.ARTIFACT_READ
            )
            else -> listOf(
                PermissionAction.READ,
                PermissionAction.WRITE,
                PermissionAction.UPDATE,
                PermissionAction.DELETE,
                PermissionAction.ARTIFACT_UPDATE,
                PermissionAction.ARTIFACT_DOWNLOAD,
                PermissionAction.ARTIFACT_READWRITE,
                PermissionAction.ARTIFACT_READ,
                PermissionAction.ARTIFACT_DELETE
            )
        }
    }

    private fun getDefaultViewerBuiltinPermission(repoName: String): List<PermissionAction> {
        return when (repoName) {
            "custom" -> listOf(
                PermissionAction.READ,
                PermissionAction.ARTIFACT_READ
            )
            "pipeline" -> listOf(
                PermissionAction.READ,
                PermissionAction.ARTIFACT_READ
            )
            else -> listOf(
                PermissionAction.READ,
                PermissionAction.ARTIFACT_READ
            )
        }
    }

    private fun canwayCheckPermission(request: CheckPermissionRequest): Boolean {
        val uid = request.uid
        val projectId = request.projectId
            ?: throw(ErrorCodeException(CommonMessageCode.PARAMETER_MISSING, "`projectId` is must not be null"))
        // todo
        val repoName = request.repoName
        val resourceType = request.resourceType
        val action = request.action
        // 用户组鉴权
        val tPermissions = permissionRepository.findByProjectIdAndReposContainsAndResourceTypeAndActionsContains(
            projectId, repoName, resourceType, action
        )

        if (tPermissions != null) {
            for (tPermission in tPermissions) {
                if (canwayCheckTPermission(uid, tPermission)) return true
            }
        }
        return false
    }

    private fun canwayCheckTPermission(uid: String, tPermission: TPermission): Boolean {
        val departments = tPermission.departments
        if (!checkDepartment(uid, departments)) return false
        val roles = tPermission.roles
        if (!checkGroup(uid, roles)) return false
        return true
    }

    private fun checkGroup(uid: String, roles: List<String>): Boolean {
        val sumUsers = mutableSetOf<String>()
        for (role in roles) {
            getUsersByGroupId(uid, role)?.let { sumUsers.addAll(it) }
        }
        return sumUsers.contains(uid)
    }

    private fun checkDepartment(uid: String, departments: List<String>): Boolean {
        val sumUsers = mutableSetOf<BkDepartmentUser>()
        for (department in departments) {
            departmentService.getUsersByDepartmentId(department.toInt())?.let { sumUsers.addAll(it) }
        }
        for (user in sumUsers) {
            if (user.username == uid) return true
        }
        return false
    }

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
    }
}
