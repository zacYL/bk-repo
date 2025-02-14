package com.tencent.bkrepo.common.metadata.model

import com.tencent.bkrepo.common.artifact.pojo.RepositoryType
import org.springframework.data.mongodb.core.index.CompoundIndex
import org.springframework.data.mongodb.core.index.CompoundIndexes
import org.springframework.data.mongodb.core.mapping.Document
import java.time.LocalDateTime

@Document("remote_package_whitelist")
@CompoundIndexes(
        CompoundIndex(name = "unique_index", def = "{'type':1, 'packageKey':1}", background = true, unique = true)
)
data class TRemotePackageWhitelist(
        val id: String? = null,
        val packageKey: String,
        val versions: List<String>?,
        val type: RepositoryType,
        val createdBy: String,
        val createdDate: LocalDateTime,
        val lastModifiedBy: String,
        val lastModifiedDate: LocalDateTime,
)
