package com.tencent.bkrepo.repository.cpack.controller

import com.tencent.bkrepo.common.artifact.pojo.RepositoryType

data class UpdateRemotePackageWhitelistRequest(
        val packageKey: String? = null,
        val versions: List<String>? = null,
        val type: RepositoryType? = null
) {
    init {
        packageKey?.let { require(packageKeyValid()) { "packageKey must not be blank" } }
        type?.let { require(!arrayOf(RepositoryType.GIT, RepositoryType.NONE, RepositoryType.GENERIC).contains(type)) {
            "type can not be [GIT, NONE, GENERIC]" }}
    }

    private fun packageKeyValid(): Boolean {
        return when (type) {
            RepositoryType.MAVEN -> packageKey?.matches(Regex("[a-z0-9_\\-.]+:[a-z0-9_\\-.]+")) ?: false
            else -> true
        }
    }
}