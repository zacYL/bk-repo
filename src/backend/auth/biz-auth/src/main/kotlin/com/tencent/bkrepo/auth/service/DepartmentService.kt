package com.tencent.bkrepo.auth.service

import com.tencent.bkrepo.auth.service.canway.pojo.BkChildrenDepartment

interface DepartmentService {
    fun listDepartmentById(username: String?, departmentId: Int?): List<BkChildrenDepartment>?

    fun listDepartmentByIds(username: String?, departmentIds: List<Int>): List<BkChildrenDepartment>
}
