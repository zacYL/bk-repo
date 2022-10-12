package com.tencent.bkrepo.repository.pojo.whitelist

import com.tencent.bkrepo.common.artifact.pojo.RepositoryType
import java.time.LocalDateTime

data class RemotePackageWhitelist(
        val id: String,
        val packageKey: String,
        val versions: List<String>?,
        val type: RepositoryType,
        val createdBy: String,
        val createdDate: LocalDateTime,
        val lastModifiedBy: String,
        val lastModifiedDate: LocalDateTime
)
