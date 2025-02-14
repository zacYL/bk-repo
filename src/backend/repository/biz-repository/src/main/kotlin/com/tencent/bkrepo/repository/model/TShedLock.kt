package com.tencent.bkrepo.repository.model

import org.springframework.data.mongodb.core.mapping.Document
import java.time.LocalDateTime

@Document("shed_lock")
data class TShedLock(
    var id: String? = null,
    val lockUntil: LocalDateTime,
    val lockedAt: LocalDateTime
)
