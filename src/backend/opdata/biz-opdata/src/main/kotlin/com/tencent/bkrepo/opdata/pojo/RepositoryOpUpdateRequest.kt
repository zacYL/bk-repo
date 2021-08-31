package com.tencent.bkrepo.opdata.pojo

import com.tencent.bkrepo.common.artifact.pojo.RepositoryType

data class RepositoryOpUpdateRequest(
    val projectId: String,
    val repoName: String,
    val repoType: RepositoryType,
    val visits: Long,
    val downloads: Long,
    val uploads: Long,
    val usedCapacity: Long,
    val packages: Long
)
