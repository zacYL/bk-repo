package com.tencent.bkrepo.repository.cpack.pojo.repo

data class OutdatedVirtualConfiguration(
    val type: String = "virtual",
    var repositoryList: List<VirtualRepositoryMember> = emptyList(),
    var deploymentRepo: String? = null
) {
    val settings: MutableMap<String, Any> = mutableMapOf()
}
