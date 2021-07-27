package com.tencent.bkrepo.auth.service.canway.pojo.bk

import com.fasterxml.jackson.annotation.JsonProperty

data class BkResponse<T> (
    val message: String,
    val code: Int,
    val data: T?,
    val result: Boolean,
    val request_id: String
)

data class BkUser(
    val username: String,
    @JsonProperty("display_name")
    val displayName: String
)

data class BkPage<T>(
    val count: Int,
    val previous: String?,
    val results: List<T>?,
    val next: String?
)

data class BkDepartment(
    val id: Int,
    val tree_id: Int?,
    val level: Int?,
    val parent: Int,
    val name: String,
    val is_deleted: Boolean?,
    val enabled: Boolean?,
    val rght: Int?,
    val lft: Int?,
    val order: Int,
    val create_time: String?,
    val update_time: String?
)

data class BkParentDepartment(
    val id: Int,
    val order: Int,
    val name: String,
    val ancestor_name: String,
    val parent: String?,
    val has_children: Boolean,
    val children: List<BkChildrenDepartment>?

)

data class BkChildrenDepartment(
    val id: String,
    val name: String,
    val order: Int?,
    val parent: Int?,
    val has_children: Boolean?
)
