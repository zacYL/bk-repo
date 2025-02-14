package com.tencent.bkrepo.auth.pojo.software.request

data class UseUnitDeleteRequest(
    val user: Set<String>,
    val department: Set<String>
)
