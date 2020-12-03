package com.tencent.bkrepo.auth.service.canway.pojo

data class CanwayRoleResourceRequest(
    // role id
    val groupId: String,
    val memberList: List<Member>
) {
    data class Member(
        val code: String,
        val type: String
    )
}
