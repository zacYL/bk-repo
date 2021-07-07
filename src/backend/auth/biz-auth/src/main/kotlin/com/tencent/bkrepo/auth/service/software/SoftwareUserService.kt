package com.tencent.bkrepo.auth.service.software

import com.tencent.bkrepo.auth.pojo.software.UnitType
import com.tencent.bkrepo.auth.pojo.software.request.UseUnitDeleteRequest
import com.tencent.bkrepo.auth.pojo.software.response.SoftwareUseUnitResponse

interface SoftwareUserService {
    fun unit(repoName: String): SoftwareUseUnitResponse

    fun updatePermission(repoName: String, set: Set<String>, unitType: UnitType, push: Boolean): Boolean

    fun addUnit(repoName: String, set: Set<String>, unitType: UnitType, push: Boolean): Boolean

    fun deleteUnit(repoName: String, useUnitDeleteRequest: UseUnitDeleteRequest): Boolean
}
