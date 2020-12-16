package com.tencent.bkrepo.repository.service.canway.pojo

data class BkUserInfo(
    val code: Int,
    val permission: String?,
    val result: Boolean,
    val request_id: String,
    val message: String,
    val data: BkUserData?
)

data class BkUserData(
    val qq: String?,
    val bk_username: String,
    val language: String?,
    val wx_userid: String?,
    val time_zone: String?,
    val bk_role: Int?,
    val phone: String?,
    val email: String?,
    val chname: String?
)
