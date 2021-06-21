package com.tencent.bkrepo.common.devops.response

data class CanwayPermissionResponse(
    val belongCode: String,
    val belongInstance: String,
    val instanceCodes: Set<InstanceCode>
) {
    data class InstanceCode(
        val resourceCode: String,
        val actionCode: String,
        val resourceInstance: Set<String>
    )
}
