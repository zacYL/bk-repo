package com.tencent.bkrepo.analyst.model

import org.springframework.data.mongodb.core.index.CompoundIndex
import org.springframework.data.mongodb.core.index.CompoundIndexes
import org.springframework.data.mongodb.core.mapping.Document
import java.time.LocalDateTime

@Document("vul_rule")
@CompoundIndexes(
    CompoundIndex(name = "vulId_index", def = "{'vulId': 1}", unique = true, background = true)
)
data class TVulRule(
    val id: String? = null,
    val vulId: String,
    val pass: Boolean,
    val description: String?,
    var createdBy: String,
    var createdDate: LocalDateTime
)
