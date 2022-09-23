package com.tencent.bkrepo.repository.cpack.controller

import com.tencent.bkrepo.common.artifact.pojo.RepositoryType
import javax.validation.ValidationException

data class CreateRemotePackageWhitelistRequest(
        val packageKey: String,
        val versions: List<String>?,
        val type: RepositoryType
) {
    init {
        require(packageKeyValid()) { throw ValidationException("packageKey is invalid") }
        require(!arrayOf(RepositoryType.GIT, RepositoryType.NONE, RepositoryType.GENERIC).contains(type)) {
            "type can not be [GIT, NONE, GENERIC]" }
    }

    private fun packageKeyValid(): Boolean {
        return when (type) {
            RepositoryType.MAVEN -> packageKey.matches(Regex("[a-z0-9_\\-.]+:[a-z0-9_\\-.]+"))
            else -> true
        }
    }
}