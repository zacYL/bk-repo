package com.tencent.bkrepo.analyst.model

import org.springframework.data.mongodb.core.index.CompoundIndex
import org.springframework.data.mongodb.core.index.CompoundIndexes
import org.springframework.data.mongodb.core.mapping.Document
import java.time.LocalDateTime

@Document("cve_whitelist")
@CompoundIndexes(
    CompoundIndex(name = "cveId_index", def = "{'cveId': 1}", unique = true, background = true)
)
data class TCveWhitelist(
    val id: String? = null,
    val cveId: String,
    val description: String?,
    var createdBy: String,
    var createdDate: LocalDateTime,
    var lastModifiedBy: String,
    var lastModifiedDate: LocalDateTime
)
