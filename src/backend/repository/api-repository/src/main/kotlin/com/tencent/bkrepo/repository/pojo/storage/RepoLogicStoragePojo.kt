package com.tencent.bkrepo.repository.pojo.storage

import com.tencent.bkrepo.common.artifact.pojo.RepositoryCategory
import com.tencent.bkrepo.common.artifact.pojo.RepositoryType

/**
 * 仓库磁盘列表
 */
data class RepoLogicStoragePojo(
    val projectId: String,
    val repoName: String,
    val type: RepositoryType,
    val category: RepositoryCategory,
    var size: String,
    val count: Long
)
