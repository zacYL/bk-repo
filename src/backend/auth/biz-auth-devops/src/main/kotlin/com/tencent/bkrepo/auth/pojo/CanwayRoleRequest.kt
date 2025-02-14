package com.tencent.bkrepo.auth.pojo

data class CanwayRoleRequest(
    val owner: Owner
){
    data class Owner(
        val scopeCode: String,
        val scopeId: String
    )
}


