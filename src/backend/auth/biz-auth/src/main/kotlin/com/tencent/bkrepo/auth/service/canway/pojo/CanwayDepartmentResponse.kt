package com.tencent.bkrepo.auth.service.canway.pojo

class CanwayDepartmentResponse<T> (
    val message: String,
    val code: Int,
    val data: T,
    val result: Boolean,
    val request_id: String
)

data class CanwayDepartmentPage(
    val count: Int,
    val previous: String?,
    val results: List<CanwayDepartmentPojo>?,
    val next: String?
)

data class CanwayDepartmentPojo(
    val id: Int,
    val tree_id: Int,
    val level: Int,
    val parent: Int?,
    val name: String,
    val is_deleted: Boolean,
    val rght: Int,
    val lft: Int,
    val order: Int,
    val create_time: String,
    val update_time: String
)

data class CanwayParentDepartmentPojo(
    val id: Int,
    val order: Int,
    val name: String,
    val ancestor_name: String,
    val parent: String?,
    val has_children: Boolean,
    val children: List<CanwayChildrenDepartmentPojo>?

)

data class CanwayChildrenDepartmentPojo(
    val id: Int,
    val name: String,
    val order: Int?,
    val parent: Int?,
    val has_children: Boolean?
)
