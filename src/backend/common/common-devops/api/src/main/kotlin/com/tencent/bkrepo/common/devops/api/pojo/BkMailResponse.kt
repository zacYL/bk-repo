package com.tencent.bkrepo.common.devops.api.pojo

data class BkMailResponse(
    val message: String?,
    val code: Int,
    val data: Any?,
    val result: Boolean?,
    val request_id: String?

)
