package com.tencent.bkrepo.opdata.pojo.response

import com.tencent.bkrepo.common.artifact.pojo.RepositoryType

data class RepoVisitData(
    val projectId: String,
    val repoName: String,
    val repoType: RepositoryType,
    val downloads: Long,
    val uploads: Long
)
