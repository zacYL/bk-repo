package com.tencent.bkrepo.auth.pojo.permission

import com.tencent.bkrepo.auth.constant.AuthRoleType
import com.tencent.bkrepo.auth.pojo.role.SlimNamedRoleVO
import com.tencent.bkrepo.auth.pojo.role.UserRelatedRoleStatusVO

class ProjectUserPermissionSourceVO(
    var user: Boolean = false,
    val roles: List<SlimNamedRoleVO>? = null,
    var projectRole: UserRelatedRoleStatusVO = UserRelatedRoleStatusVO(),
    var tenantProjectRole: UserRelatedRoleStatusVO = UserRelatedRoleStatusVO(),
    var tenantTemplateRole: UserRelatedRoleStatusVO = UserRelatedRoleStatusVO(),
    var systemTemplateRole: UserRelatedRoleStatusVO = UserRelatedRoleStatusVO(),
) {
    fun getStatusByRoleType(roleType: String): UserRelatedRoleStatusVO? {
        return when (roleType) {
            AuthRoleType.PROJECT_ROLE -> projectRole
            AuthRoleType.TENANT_PROJECT_ROLE -> tenantProjectRole
            AuthRoleType.TENANT_TEMPLATE_ROLE -> tenantTemplateRole
            AuthRoleType.SYSTEM_TEMPLATE_ROLE -> systemTemplateRole
            else -> null
        }
    }
}