package com.tencent.bkrepo.auth.service

import com.tencent.bkrepo.auth.pojo.CanwayGroup
import com.tencent.bkrepo.auth.pojo.DevopsDepartment

interface DevopsUserService {
    fun usersByProjectId(projectId: String): List<String>?

    fun groupsByProjectId(projectId: String): List<CanwayGroup>?

    fun departmentsByProjectId(projectId: String): List<DevopsDepartment>?

    fun childrenDepartments(departmentId: String): List<DevopsDepartment>?
}
