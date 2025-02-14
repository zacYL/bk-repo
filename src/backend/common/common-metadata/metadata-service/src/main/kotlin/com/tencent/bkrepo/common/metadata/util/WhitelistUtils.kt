package com.tencent.bkrepo.common.metadata.util

import com.tencent.bkrepo.common.api.exception.ErrorCodeException
import com.tencent.bkrepo.common.api.message.CommonMessageCode
import com.tencent.bkrepo.common.artifact.pojo.RepositoryType


object WhitelistUtils {

    fun optionalType(): List<RepositoryType> {
        return listOf(RepositoryType.MAVEN, RepositoryType.NPM)
    }

    /**
     * [packageKey] is the package name of the artifact
     * [type] is the type of the artifact
     * @return true if valid
     */
    fun packageKeyValid(packageKey: String, type: RepositoryType): Boolean {
        if(packageKey.isBlank()) {
            return false
        }
        return when (type) {
            RepositoryType.MAVEN -> packageKey.matches(Regex("[a-zA-Z0-9_\\-.]+:[a-zA-Z0-9_\\-.]+"))
            RepositoryType.NPM -> true
            else -> false
        }
    }

    /**
     * [type] is the type of the artifact
     */
    fun typeValid(type: RepositoryType) {
        if(!optionalType().contains(type))
            throw ErrorCodeException(CommonMessageCode.PARAMETER_INVALID, type.name)
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
