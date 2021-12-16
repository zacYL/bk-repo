package com.tencent.bkrepo.repository.pojo.software

import io.swagger.annotations.ApiModel

@ApiModel("节点搜索结果总览")
data class CpackNodeOverviewResponse(
    val list: List<RepoNodeOverview>,
    val sum: Long
) {
    data class RepoNodeOverview(
        val repoName: String,
        val nodes: Long
    )
}
