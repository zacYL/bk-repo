package com.tencent.bkrepo.auth.pojo.permission

import com.tencent.bkrepo.auth.pojo.enums.PermissionAction
import com.tencent.bkrepo.auth.pojo.enums.ResourceType

data class CheckPermissionContext(
    var userId: String,
    var roles: List<String>,
    var resourceType: ResourceType,
    var action: PermissionAction,
    var projectId: String,
    var repoName: String? = null,
    var path: String? = null,
)