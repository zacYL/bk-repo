package com.tencent.bkrepo.common.devops.pojo

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

data class BkDepartmentId(
    val id: Int
)

data class BkChildrenDepartment(
    val id: String,
    val name: String,
    val order: Int?,
    val parent: Int?,
    val level: Int,
    val has_children: Boolean?,
    var permission: Boolean = false,
    var children: MutableList<BkChildrenDepartment> = mutableListOf(),
    var parentDepartmentIds: MutableList<String> = mutableListOf()
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || other !is BkChildrenDepartment) return false
        return id == other.id && name == other.name
    }

    override fun hashCode(): Int {
        return id.hashCode()
    }
}
