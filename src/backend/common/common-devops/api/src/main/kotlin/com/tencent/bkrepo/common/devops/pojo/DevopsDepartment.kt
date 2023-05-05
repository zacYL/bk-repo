package com.tencent.bkrepo.common.devops.pojo

data class DevopsDepartment(
    val id: String,
    val name: String,
    val parentId: String? = null,
    val children: List<DevopsDepartment>? = listOf()
)
