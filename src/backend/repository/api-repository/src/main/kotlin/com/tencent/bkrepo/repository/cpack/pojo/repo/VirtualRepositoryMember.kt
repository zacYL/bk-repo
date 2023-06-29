package com.tencent.bkrepo.repository.cpack.pojo.repo

import com.tencent.bkrepo.common.artifact.pojo.RepositoryCategory

data class VirtualRepositoryMember(
    var projectId: String?,
    val name: String,
    var category: RepositoryCategory?
)
