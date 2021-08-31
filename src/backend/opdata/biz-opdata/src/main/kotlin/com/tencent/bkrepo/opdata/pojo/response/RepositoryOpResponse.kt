package com.tencent.bkrepo.opdata.pojo.response

import java.time.LocalDateTime

data class RepositoryOpResponse(
    val projectId: String,
    val repoName: String,
    val capacity: Long,
    val visits: Long,
    val downloads: Long,
    val uploads: Long,
    val usedCapacity: Long,
    val packages: Long,
    val latestModifiedDate: LocalDateTime
)
