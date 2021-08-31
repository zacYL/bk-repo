package com.tencent.bkrepo.opdata.pojo

import com.tencent.bkrepo.repository.pojo.packages.PackageType
import java.time.LocalDateTime

data class VersionOpUpdateRequest(
    val projectId: String,
    val repoName: String,
    val packageKey: String,
    val type: PackageType,
    val lastModifiedDate: LocalDateTime,
    val packageId: String,
    val packageName: String,
    val packageVersion: String,
    val size: Long,
    val downloads: Long
)
