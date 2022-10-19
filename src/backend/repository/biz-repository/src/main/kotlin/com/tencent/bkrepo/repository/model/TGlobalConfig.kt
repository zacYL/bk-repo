package com.tencent.bkrepo.repository.model

import org.springframework.data.mongodb.core.mapping.Document
import java.time.LocalDateTime

/**
 * 全局配置模型
 */
@Document("global_config")
data class TGlobalConfig(
    var id: String? = null,
    var createdBy: String,
    var createdDate: LocalDateTime,
    var lastModifiedBy: String,
    var lastModifiedDate: LocalDateTime,

    var replicationNetworkRate: Long?
)
