package com.tencent.bkrepo.auth.service

import com.tencent.bkrepo.common.devops.pojo.BkChildrenDepartment
import com.tencent.bkrepo.common.devops.pojo.BkDepartmentUser

interface DepartmentService {
    fun listDepartmentById(userId: String, username: String?, departmentId: Int?): List<BkChildrenDepartment>?

    fun listDepartmentByIds(userId: String, username: String?, departmentIds: List<Int>): List<BkChildrenDepartment>?

    fun getUsersByDepartmentId(username: String?, departmentId: Int): List<BkDepartmentUser>?
}
