package com.tencent.bkrepo.auth.pojo

data class MigrateTokenVO(
    val userId: String,
    val tokenName: String,
    val token: String
)
