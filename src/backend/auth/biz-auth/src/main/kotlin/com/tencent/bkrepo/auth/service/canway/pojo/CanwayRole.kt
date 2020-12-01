package com.tencent.bkrepo.auth.service.canway.pojo

data class CanwayRole(
    val id: String,
    val parentId: String,
    val name: String,
    val description: String,
    val service: String,
    val belongCode: String,
    val belongInstance: String,
    val createdUser: String,
    val createdTime: Long,
    val updatedUser: String,
    val updatedTime: Long,
    val count: Int
)
