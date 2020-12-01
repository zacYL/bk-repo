package com.tencent.bkrepo.auth.service.canway.pojo

data class UserResourceAuthResponse(
    val belongCode: String, // project,
    val belongInstance: String, // deploy,
    val instanceCodes: List<ResourcesAction>?
) {
    data class ResourcesAction(
        val resourceCode: String, // pipeline
        val actionCode: String, // list
        val resourceInstance: Set<String>?
    )
}
