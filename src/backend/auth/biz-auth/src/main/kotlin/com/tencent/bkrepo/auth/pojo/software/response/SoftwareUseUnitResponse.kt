package com.tencent.bkrepo.auth.pojo.software.response

import com.tencent.bkrepo.auth.pojo.software.SoftwareUseUnit

data class SoftwareUseUnitResponse(
    val user: Set<SoftwareUseUnit>,
    val department: Set<SoftwareUseUnit>
)