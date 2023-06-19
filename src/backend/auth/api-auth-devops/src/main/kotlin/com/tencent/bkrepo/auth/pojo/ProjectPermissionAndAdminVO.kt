package com.tencent.bkrepo.auth.pojo

class ProjectPermissionAndAdminVO(
    val permissions: List<InstancePermissionVO>,
    val systemAdmin: Boolean,
    val tenantAdmin: Boolean,
    val projectAdmin: Boolean
) {
    fun isAdmin(): Boolean = projectAdmin || tenantAdmin || systemAdmin
}
