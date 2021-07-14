package com.tencent.bkrepo.opdata.pojo.response

import com.tencent.bkrepo.common.artifact.pojo.RepositoryType


data class RepoCapacityData(
    val projectId: String,
    val repoName: String,
    val repoType: RepositoryType,
    val capacityLimit: Long,
    val usedCapacity: Long
)
