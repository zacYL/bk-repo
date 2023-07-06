package com.tencent.bkrepo.auth.pojo.permission

data class ProjectPermissionAndAdminVO(
    val permissions: List<InstancePermissionVO>,
    val systemAdmin: Boolean,
    val tenantAdmin: Boolean,
    val projectAdmin: Boolean
)
