package com.tencent.bkrepo.repository.pojo.software

data class CpackGlobalSearchPojo(
    val id: RepoInfo,
    val count: Long
) {
    data class RepoInfo(
        val projectId: String,
        val repoName: String
    )
}
