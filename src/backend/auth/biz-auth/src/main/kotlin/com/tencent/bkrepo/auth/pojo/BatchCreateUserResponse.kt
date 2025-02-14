package com.tencent.bkrepo.auth.pojo

data class BatchCreateUserResponse(
    val success: Int,
    val failed: Int,
    val failedUsers: Set<String>
)
