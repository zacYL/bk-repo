package com.tencent.bkrepo.auth.pojo

data class CanwayBkrepoPermission(
    val resourceCode: String,
    var actionCodes: List<String>
)
