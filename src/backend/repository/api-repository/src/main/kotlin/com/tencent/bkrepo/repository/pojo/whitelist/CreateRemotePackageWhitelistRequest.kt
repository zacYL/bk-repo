package com.tencent.bkrepo.repository.pojo.whitelist

import com.tencent.bkrepo.common.artifact.pojo.RepositoryType

data class CreateRemotePackageWhitelistRequest(
        val packageKey: String,
        val versions: List<String>?,
        val type: RepositoryType
)