package net.canway.devops.auth.pojo

import com.tencent.bkrepo.auth.constant.AuthConstant




class UserPermissionValidateDTO(
    val userId: String,
    val instanceId: String = AuthConstant.ANY_RESOURCE_CODE,
    val resourceCode: String,
    val actionCodes: List<String>
)
