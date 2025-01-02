package com.tencent.bkrepo.analyst.pojo.response

import java.time.LocalDateTime

data class VulRuleInfo(
    val vulId: String,
    val pass: Boolean,
    val description: String?,
    val createdBy: String,
    val createdDate: LocalDateTime
)
