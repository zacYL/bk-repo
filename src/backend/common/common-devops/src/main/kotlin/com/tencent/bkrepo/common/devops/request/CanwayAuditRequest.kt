package com.tencent.bkrepo.common.devops.request

import com.fasterxml.jackson.annotation.JsonProperty
import com.tencent.bkrepo.common.api.constant.ANONYMOUS_USER

data class CanwayAuditRequest(
    val userId: String = ANONYMOUS_USER,
    val eventKey: String,
    val tenantId: String? = null,
    val projectId: String,
    val sourceIp: String,
    val createdTime: Long = System.currentTimeMillis(),
    val status: String,
    val extend: Extend
) {
    data class Extend(
        @JsonProperty("OPERATE_OBJECT_NAME")
        val name: String,
        @JsonProperty("OPERATE_OBJECT_ID")
        val id: String,
        @JsonProperty("OPERATE_OBJECT_DESCRIPTION")
        val description: String
    )
}
