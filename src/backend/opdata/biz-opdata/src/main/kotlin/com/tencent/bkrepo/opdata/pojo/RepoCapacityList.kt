package com.tencent.bkrepo.opdata.pojo

data class RepoCapacityList(
    val count: Long,
    val repos: List<RepoCapacityDetail>?
)

data class RepoCapacityDetail(
    val projectId: String,
    val name: String,
    val usedCapacity: Long,
    val quota: Long = Long.MAX_VALUE
)
