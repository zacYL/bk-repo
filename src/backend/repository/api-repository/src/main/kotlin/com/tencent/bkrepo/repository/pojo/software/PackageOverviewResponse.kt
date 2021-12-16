package com.tencent.bkrepo.repository.pojo.software

import io.swagger.annotations.ApiModel

@ApiModel("包搜索结果总览")
data class PackageOverviewResponse(
    val list: List<RepoPackageOverview>,
    val sum: Long
) {
    data class RepoPackageOverview(
        val repoName: String,
        val packages: Long
    )
}
