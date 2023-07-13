package com.tencent.bkrepo.auth.pojo.role

data class UserRelatedRoleStatusVO(
    var associatedByUser: Boolean = false,
    var associatedByGroup: Boolean = false,
    var associatedByDepartment: Boolean = false,
)
