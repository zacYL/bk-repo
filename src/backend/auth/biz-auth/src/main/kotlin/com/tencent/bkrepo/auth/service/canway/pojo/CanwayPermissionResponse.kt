package com.tencent.bkrepo.auth.service.canway.pojo

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
