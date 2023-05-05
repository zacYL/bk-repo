package com.tencent.bkrepo.auth.service.impl

import com.tencent.bkrepo.auth.*
import com.tencent.bkrepo.auth.model.TRole
import com.tencent.bkrepo.auth.pojo.CanwayRoleRequest
import com.tencent.bkrepo.auth.pojo.enums.RoleType
import com.tencent.bkrepo.auth.pojo.role.Role
import com.tencent.bkrepo.auth.repository.RoleRepository
import com.tencent.bkrepo.auth.repository.UserRepository
import com.tencent.bkrepo.auth.service.PermissionService
import com.tencent.bkrepo.auth.service.UserService
import com.tencent.bkrepo.common.api.exception.ErrorCodeException
import com.tencent.bkrepo.common.api.message.CommonMessageCode
import com.tencent.bkrepo.common.api.util.readJsonString
import com.tencent.bkrepo.common.api.util.toJsonString
import com.tencent.bkrepo.common.devops.client.DevopsClient
import com.tencent.bkrepo.common.devops.conf.DevopsConf
import com.tencent.bkrepo.common.devops.pojo.CanwayGroup
import com.tencent.bkrepo.common.devops.pojo.request.CanwayUserGroupRequest
import com.tencent.bkrepo.common.devops.pojo.response.CanwayResponse
import com.tencent.bkrepo.common.devops.pojo.response.CanwayTenantGroupResponse
import com.tencent.bkrepo.common.devops.util.http.SimpleHttpUtils
import com.tencent.bkrepo.common.service.util.HttpContextHolder
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.mongodb.core.MongoTemplate
import org.springframework.data.mongodb.core.query.Criteria
import org.springframework.data.mongodb.core.query.Query
import org.springframework.data.mongodb.core.query.Update



class CanwayRoleServiceImpl(
    private val roleRepository: RoleRepository,
    userService: UserService,
    userRepository: UserRepository,
    private val mongoTemplate: MongoTemplate,
    permissionService: PermissionService
) : CpackRoleServiceImpl(roleRepository, userService, userRepository, mongoTemplate, permissionService) {

    @Autowired
    lateinit var devopsConf: DevopsConf

    @Autowired
    lateinit var devopsClient: DevopsClient

    override fun listRoleByProject(projectId: String, repoName: String?): List<Role> {
        // 插入用户组
        val tenantId = getTenantId()
        val groups = getGroupByTenantId(tenantId)
        checkGroups(groups)
        return super.listRoleByProject(projectId, repoName)
    }

    private fun checkGroups(groups: List<CanwayGroup>?) {
        groups?.forEach { group ->
            roleRepository.findTRoleById(group.id).apply {
                if (this == null) {
                    roleRepository.insert(
                        TRole(
                            id = group.id,
                            roleId = group.id,
                            name = group.name,
                            type = RoleType.SYSTEM,
                            projectId = null,
                            admin = false
                        )
                    )
                }
                if (this != null && this.name != group.name) {
                    mongoTemplate.updateFirst(
                        Query(Criteria.where(TRole::roleId.name).`is`(group.id)),
                        Update().set(TRole::name.name, group.name),
                        TRole::class.java
                    )
                }
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
            if (cookie.name == CI_TENANT) tenant = cookie.value
        }
        if (tenant == null) throw ErrorCodeException(CommonMessageCode.HEADER_MISSING)
        return tenant
    }

    /**
     * 查询租户下用户组
     */
    private fun getGroupByTenantId(tenantId: String): List<CanwayGroup> {
        logger.info("inquire [tenantId:${tenantId}] ")
        val uri = String.format(groupApi)
        val requestUrl = getRequestUrl(uri)
        val owner = CanwayRoleRequest.Owner(TENANT_SCOPECODE,tenantId)
        val responseContent = SimpleHttpUtils.doPost(requestUrl,CanwayRoleRequest(owner).toJsonString()).content
        val groupResponse=responseContent.readJsonString<CanwayResponse<List<CanwayTenantGroupResponse>>>().data ?: listOf()
        if (groupResponse.isEmpty()) return listOf()
        val groupInformations=devopsClient.groupInformationByGroupIds(CanwayUserGroupRequest(groupIds=groupResponse.map { it.id })) ?: listOf()
        val canwayGroup= mutableListOf<CanwayGroup>()
        groupResponse.forEach {group->
            if (group.deleted) return@forEach
            canwayGroup.add(CanwayGroup(
                id=group.id,
                name=group.name,
                description= group.desc,
                tenantCode= group.ownerId,
                users=groupInformations.filter { group.id==it.userGroupId }.map { it.userId }
            ))
        }
        return canwayGroup
    }

    private fun getRequestUrl(uri: String): String {
        val devopsHost = devopsConf.devopsHost
        return "${devopsHost.removeSuffix("/")}$ciAuth$ciApi$uri"
    }

    override fun systemRolesByProjectId(projectId: String): List<Role> {
        val projectGroups = devopsClient.groupsByProjectId(projectId) ?: listOf()
        checkGroups(projectGroups)
        val projectGroupIds = projectGroups.map { it.id }
        return systemRoles().filter { projectGroupIds.contains(it.id) }
    }

    companion object {
        const val groupApi = "/service/custom/user_group/query"
        private val logger = LoggerFactory.getLogger(CanwayRoleServiceImpl::class.java)
    }
}
