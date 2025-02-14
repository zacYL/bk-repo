package com.tencent.bkrepo.auth.pojo

/**
 * CI 权限中心用户列表
 */
data class DevopsUser(
    val id: String,
    val displayName: String,
    val email: String?,
    val weChatId: String?
)
