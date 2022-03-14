package com.tencent.bkrepo.scanner.pojo.context

import com.tencent.bkrepo.common.artifact.pojo.RepositoryType

data class ArtifactPlanContext(
    val projectId: String,
    val repoType: RepositoryType,
    val repoName: String,
    val packageKey: String?,
    val version: String?,
    val fullPath: String?
)
