package com.tencent.bkrepo.repository.util

import com.tencent.bkrepo.common.api.exception.ErrorCodeException
import com.tencent.bkrepo.common.api.message.CommonMessageCode
import com.tencent.bkrepo.common.artifact.pojo.RepositoryType


object WhitelistUtils {

    fun optionalType(): List<RepositoryType> {
        return listOf(RepositoryType.MAVEN, RepositoryType.NPM)
    }
    fun packageKeyValid(packageKey: String, type: RepositoryType): Boolean {
        return when (type) {
            RepositoryType.MAVEN -> packageKey.matches(Regex("[a-zA-Z0-9_\\-.]+:[a-zA-Z0-9_\\-.]+"))
            RepositoryType.NPM -> true
            else -> false
        }
    }

    private fun formatInfo(type: RepositoryType): String {
        return when (type) {
            RepositoryType.MAVEN -> "groupId:artifactId"
            RepositoryType.NPM -> "xxxx"
            else -> "not support"
        }
    }

    fun packageKeyValidThrow(packageKey: String, type: RepositoryType) {
        if (!packageKeyValid(packageKey, type)) {
            throw ErrorCodeException(CommonMessageCode.PACKAGEKEY_INVALID, packageKey, formatInfo(type))
        }
    }
}