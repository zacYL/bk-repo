package com.tencent.bkrepo.auth.service.canway

import com.tencent.bkrepo.auth.model.TRole
import com.tencent.bkrepo.auth.pojo.enums.RoleType
import com.tencent.bkrepo.auth.pojo.role.Role
import com.tencent.bkrepo.auth.repository.RoleRepository
import com.tencent.bkrepo.auth.service.canway.conf.CanwayAuthConf
import com.tencent.bkrepo.auth.service.canway.http.CanwayHttpUtils
import com.tencent.bkrepo.auth.service.canway.pojo.CanwayGroup
import com.tencent.bkrepo.auth.service.canway.pojo.CanwayResponse
import com.tencent.bkrepo.auth.service.local.RoleServiceImpl
import com.tencent.bkrepo.common.api.exception.ErrorCodeException
import com.tencent.bkrepo.common.api.message.CommonMessageCode
import com.tencent.bkrepo.common.api.util.readJsonString
import com.tencent.bkrepo.common.service.util.HttpContextHolder
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class CanwayRoleServiceImpl(
    roleRepository: RoleRepository,
    private val canwayAuthConf: CanwayAuthConf
) : RoleServiceImpl(roleRepository) {
    override fun listRoleByProject(projectId: String, repoName: String?): List<Role> {
        // 插入用户组
        val tenantId = getTenantId()
        val groups = getGroupByTenantId(tenantId)
        checkGroups(groups, projectId)
        return super.listRoleByProject(projectId, repoName)
    }

    private fun checkGroups(groups: List<CanwayGroup>?, projectId: String) {
        groups ?: return
        for (group in groups) {
            if (roleRepository.findFirstById(group.name) == null) {
                roleRepository.insert(
                    TRole(
                        roleId = group.name,
                        name = group.name,
                        type = RoleType.PROJECT,
                        projectId = projectId,
                        repoName = null,
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
        val devopsHost = canwayAuthConf.devopsHost ?: throw ErrorCodeException(CommonMessageCode.PARAMETER_MISSING)
        return "${devopsHost.removeSuffix("/")}$ci$ciApi$uri"
    }

    companion object {
        val logger: Logger = LoggerFactory.getLogger(CanwayRoleServiceImpl::class.java)
        const val groupApi = "/service/tenant/%s/group"
    }
}
