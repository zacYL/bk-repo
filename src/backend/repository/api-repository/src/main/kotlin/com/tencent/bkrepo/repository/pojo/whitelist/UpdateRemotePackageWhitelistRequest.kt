package com.tencent.bkrepo.repository.pojo.whitelist

import com.tencent.bkrepo.common.artifact.pojo.RepositoryType

data class UpdateRemotePackageWhitelistRequest(
        val packageKey: String? = null,
        val versions: List<String>? = null,
        val type: RepositoryType? = null
)