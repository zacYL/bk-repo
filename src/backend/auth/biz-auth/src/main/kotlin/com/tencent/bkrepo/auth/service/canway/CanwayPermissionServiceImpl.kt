package com.tencent.bkrepo.auth.service.canway

import com.tencent.bkrepo.auth.model.TPermission
import com.tencent.bkrepo.auth.pojo.permission.CheckPermissionRequest
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
        // canway 权限中心 权限校验
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
        }
        return super.checkPermission(request)
    }

    private fun canwayCheckPermission(request: CheckPermissionRequest): Boolean {
        val uid = request.uid
        val projectId = request.projectId
            ?: throw(ErrorCodeException(CommonMessageCode.PARAMETER_MISSING, "`projectId` is must not be null"))
        // todo
        val repoName = request.repoName!!
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
