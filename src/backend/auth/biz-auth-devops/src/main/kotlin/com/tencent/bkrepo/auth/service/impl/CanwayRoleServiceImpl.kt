package com.tencent.bkrepo.auth.service.impl

import com.tencent.bkrepo.auth.model.TRole
import com.tencent.bkrepo.auth.pojo.enums.RoleType
import com.tencent.bkrepo.auth.pojo.role.Role
import com.tencent.bkrepo.auth.repository.RoleRepository
import com.tencent.bkrepo.auth.ciApi
import com.tencent.bkrepo.auth.ciTenant
import com.tencent.bkrepo.auth.pojo.CanwayGroup
import com.tencent.bkrepo.auth.ci
import com.tencent.bkrepo.auth.repository.UserRepository
import com.tencent.bkrepo.auth.service.UserService
import com.tencent.bkrepo.auth.service.local.RoleServiceImpl
import com.tencent.bkrepo.common.api.exception.ErrorCodeException
import com.tencent.bkrepo.common.api.message.CommonMessageCode
import com.tencent.bkrepo.common.api.util.readJsonString
import com.tencent.bkrepo.common.devops.api.conf.DevopsConf
import com.tencent.bkrepo.common.devops.api.pojo.response.CanwayResponse
import com.tencent.bkrepo.common.devops.api.util.http.CanwayHttpUtils
import com.tencent.bkrepo.common.service.util.HttpContextHolder
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.data.mongodb.core.MongoTemplate

class CanwayRoleServiceImpl(
    private val roleRepository: RoleRepository,
    private val userService: UserService,
    private val userRepository: UserRepository,
    private val mongoTemplate: MongoTemplate,
    private val devopsConf: DevopsConf
) : RoleServiceImpl(roleRepository, userService, userRepository, mongoTemplate) {
    override fun listRoleByProject(projectId: String, repoName: String?): List<Role> {
        // 插入用户组
        val tenantId = getTenantId()
        val groups = getGroupByTenantId(tenantId)
        checkGroups(groups, projectId, repoName)
        return super.listRoleByProject(projectId, repoName)
    }

    private fun checkGroups(groups: List<CanwayGroup>?, projectId: String, repoName: String?) {
        groups ?: return
        for (group in groups) {
            if (repoName?.let
                { roleRepository.findFirstByRoleIdAndProjectIdAndRepoName(group.id, projectId, it) } == null
            ) {
                roleRepository.insert(
                    TRole(
                        roleId = group.id,
                        name = group.name,
                        type = RoleType.REPO,
                        projectId = projectId,
                        repoName = repoName,
                        admin = false
                    )
                )
            }
        }
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
     * 查询租户下用户组
     */
    private fun getGroupByTenantId(tenantId: String): List<CanwayGroup>? {
        val uri = String.format(groupApi, tenantId)
        val requestUrl = getRequestUrl(uri)
        val responseContent = CanwayHttpUtils.doGet(requestUrl).content
        return responseContent.readJsonString<CanwayResponse<List<CanwayGroup>>>().data
    }

    private fun getRequestUrl(uri: String): String {
        val devopsHost = devopsConf.devopsHost
        return "${devopsHost.removeSuffix("/")}$ci$ciApi$uri"
    }

    companion object {
        val logger: Logger = LoggerFactory.getLogger(CanwayRoleServiceImpl::class.java)
        const val groupApi = "/service/tenant/%s/group"
    }
}
