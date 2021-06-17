package com.tencent.bkrepo.repository.pojo.metric

import com.tencent.bkrepo.common.artifact.pojo.RepositoryType

data class PackageDetail(
    val projectId: String,
    val repoName: String,
    val packageName: String,
    val key: String,
    val type: RepositoryType,
    val name: String,
    val downloads: Long,
    val size: Long
)
