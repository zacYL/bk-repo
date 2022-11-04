package com.tencent.bkrepo.repository.model

import com.tencent.bkrepo.repository.pojo.config.ConfigType
import org.springframework.data.mongodb.core.index.CompoundIndex
import org.springframework.data.mongodb.core.index.CompoundIndexes
import org.springframework.data.mongodb.core.mapping.Document
import java.time.LocalDateTime

/**
 * 全局配置模型
 */
@Document("global_config")
@CompoundIndexes(
    CompoundIndex(name = "type_idx", def = "{'type': 1}", unique = true)
)
data class TGlobalConfig(
    var id: String? = null,
    var createdBy: String,
    var createdDate: LocalDateTime,
    var lastModifiedBy: String,
    var lastModifiedDate: LocalDateTime,

    val type: ConfigType,
    val configuration: String,
)
