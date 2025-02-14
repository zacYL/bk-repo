package com.tencent.bkrepo.common.metadata.model

import com.tencent.bkrepo.common.artifact.pojo.RepositoryType
import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.mapping.Document
import java.time.LocalDateTime

@Document("whitelist_switch")
data class TWhitelistSwitch (
        @Id
        var type: RepositoryType,
        val createdBy: String,
        val createdDate: LocalDateTime
)
