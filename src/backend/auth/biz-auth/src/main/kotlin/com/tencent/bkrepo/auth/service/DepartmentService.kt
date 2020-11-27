package com.tencent.bkrepo.auth.service

import com.tencent.bkrepo.auth.service.canway.pojo.CanwayChildrenDepartmentPojo

interface DepartmentService {
    fun listDepartmentById(username: String?, departmentId: Int?): List<CanwayChildrenDepartmentPojo>?

    fun listDepartmentByIds(username: String?, departmentIds: List<Int>): List<CanwayChildrenDepartmentPojo>
}
