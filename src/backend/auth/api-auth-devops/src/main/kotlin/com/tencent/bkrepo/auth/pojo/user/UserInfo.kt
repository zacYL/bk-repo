package com.tencent.bkrepo.auth.pojo.user

data class UserInfo(
    val userId: String?,
    var displayName: String?,
    val email: String?,
    val telephone: String?,
    val status: Boolean = false,
    val staffStatus: Boolean = false,
    val source: String,
    var weChatId: String?,
    val openId: String?,
    var dingTalkId: String? = ""
)
