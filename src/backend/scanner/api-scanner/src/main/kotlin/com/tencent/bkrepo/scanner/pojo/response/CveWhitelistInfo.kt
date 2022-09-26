package com.tencent.bkrepo.scanner.pojo.response

import java.time.LocalDateTime

data class CveWhitelistInfo(
    val cveId: String,
    val description: String?,
    val createdBy: String,
    val createdDate: LocalDateTime,
    val lastModifiedBy: String,
    val lastModifiedDate: LocalDateTime
)
