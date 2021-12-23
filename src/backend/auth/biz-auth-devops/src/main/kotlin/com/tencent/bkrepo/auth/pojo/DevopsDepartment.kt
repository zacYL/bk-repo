package com.tencent.bkrepo.auth.pojo

data class DevopsDepartment(
    val id: String,
    val name: String,
    val parentId: String?,
    val children: List<Any>?
)
