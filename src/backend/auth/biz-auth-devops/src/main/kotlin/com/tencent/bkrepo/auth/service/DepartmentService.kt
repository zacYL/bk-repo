package com.tencent.bkrepo.auth.service

import com.tencent.bkrepo.auth.pojo.BkDepartmentUser
import com.tencent.bkrepo.common.devops.pojo.BkChildrenDepartment

interface DepartmentService {
    fun listDepartmentById(username: String?, departmentId: Int?): List<BkChildrenDepartment>?

    fun listDepartmentByIds(username: String?, departmentIds: List<Int>): List<BkChildrenDepartment>

    fun getUsersByDepartmentId(username: String?, departmentId: Int): Set<BkDepartmentUser>?
}
