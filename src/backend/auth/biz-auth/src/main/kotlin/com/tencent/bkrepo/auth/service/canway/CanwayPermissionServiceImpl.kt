package com.tencent.bkrepo.auth.service.canway

import com.tencent.bkrepo.auth.config.BkAuthConfig
import com.tencent.bkrepo.auth.pojo.permission.CheckPermissionRequest
import com.tencent.bkrepo.auth.pojo.permission.UpdatePermissionActionRequest
import com.tencent.bkrepo.auth.pojo.permission.UpdatePermissionDepartmentRequest
import com.tencent.bkrepo.auth.pojo.permission.UpdatePermissionRoleRequest
import com.tencent.bkrepo.auth.pojo.permission.UpdatePermissionUserRequest
import com.tencent.bkrepo.auth.repository.PermissionRepository
import com.tencent.bkrepo.auth.repository.RoleRepository
import com.tencent.bkrepo.auth.repository.UserRepository
import com.tencent.bkrepo.auth.service.bkauth.BkAuthProjectService
import com.tencent.bkrepo.auth.service.bkauth.BkAuthService
import com.tencent.bkrepo.auth.service.canway.conf.CanwayAuthConf
import com.tencent.bkrepo.auth.service.canway.pojo.CanwayResponse
import com.tencent.bkrepo.auth.service.canway.pojo.UserResourceAuthResponse
import com.tencent.bkrepo.auth.service.canway.pojo.UserResourcesAuthRequest
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
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.mongodb.core.MongoTemplate

class CanwayPermissionServiceImpl constructor(
    userRepository: UserRepository,
    roleRepository: RoleRepository,
    permissionRepository: PermissionRepository,
    mongoTemplate: MongoTemplate,
    repositoryClient: RepositoryClient,
    private val bkAuthConfig: BkAuthConfig,
    private val bkAuthService: BkAuthService,
    private val bkAuthProjectService: BkAuthProjectService
) : PermissionServiceImpl(userRepository, roleRepository, permissionRepository, mongoTemplate, repositoryClient) {

    @Autowired
    lateinit var canwayAuthConf: CanwayAuthConf

    override fun checkPermission(request: CheckPermissionRequest): Boolean {
        logger.info("check permission  request : [$request] ")
        // canway 权限中心 权限校验
        return checkCanwayPermission(request)
    }

    override fun updatePermissionAction(request: UpdatePermissionActionRequest): Boolean {

        return super.updatePermissionAction(request)
    }

    override fun updatePermissionDepartment(request: UpdatePermissionDepartmentRequest): Boolean {
        return super.updatePermissionDepartment(request)
    }

    override fun updatePermissionRole(request: UpdatePermissionRoleRequest): Boolean {
        return super.updatePermissionRole(request)
    }

    override fun updatePermissionUser(request: UpdatePermissionUserRequest): Boolean {

        return super.updatePermissionUser(request)
    }

    /**
     * 获取租户
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

    private fun checkCanwayPermission(request: CheckPermissionRequest): Boolean {
        val canwayRequest = getCanwayPermissionRequest(request)

        val responseContent = HttpUtils.doRequest(OkHttpClient(), canwayRequest, 3, mutableSetOf(200)).content

        val userResources = responseContent.readJsonString<CanwayResponse<UserResourceAuthResponse>>()
            .data ?: throw ErrorCodeException(CommonMessageCode.RESOURCE_NOT_FOUND)

        val instanceList = userResources.instanceCodes ?: return false

        if (!match(request, instanceList)) return false

        return super.checkPermission(request)
    }

    private fun getCanwayPermissionRequest(request: CheckPermissionRequest): Request {
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
                            instanceCode = repoName
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

    private fun match(request: CheckPermissionRequest, instanceList: List<UserResourceAuthResponse.ResourcesAction>): Boolean {
        val repoName = request.repoName
        for (resourceAction in instanceList) {
            if (resourceAction.resourceInstance != null && resourceAction.resourceInstance.contains(repoName)) return true
        }
        return false
    }

    companion object {
        val logger: Logger = LoggerFactory.getLogger(CanwayPermissionServiceImpl::class.java)
        val mediaType = MediaType.parse("application/json; charset=utf-8")
        const val checkPermissionApi = "$ci/api/service/resource_instance/query"
        const val ciTenant = "X-DEVOPS-TENANT-ID"
        const val ciProject = "X-DEVOPS-PROJECT-ID"

        // 在 canway 权限中心注册的资源名
        const val ciResourceCode = "bkrepo"
    }
}
