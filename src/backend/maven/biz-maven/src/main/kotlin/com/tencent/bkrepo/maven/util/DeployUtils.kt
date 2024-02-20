package com.tencent.bkrepo.maven.util

import com.tencent.bkrepo.common.api.exception.ErrorCodeException
import com.tencent.bkrepo.maven.enum.MavenMessageCode
import com.tencent.bkrepo.maven.pojo.request.MavenWebDeployRequest

/**
 * web 上传工具类
 * 对groupId, artifactId, version, classifier, type进行校验
 */
object DeployUtils {
    private const val ILLEGAL_VERSION_CHARS = "\\/:\"<>|?*[](){},"
    private const val ValidIdRegex = "[a-zA-Z0-9_\\-.]+"
    private const val ValidId_CHARS = "a-zA-Z0-9_-."

    private fun validateVersion(version: String): Boolean {
        return version.all { it !in ILLEGAL_VERSION_CHARS }
    }

    private fun validateId(id: String): Boolean {
        return id.matches(Regex(ValidIdRegex))
    }

    fun MavenWebDeployRequest.validate() {
        this.version?.let { version ->
            if (!validateVersion(version)) throw ErrorCodeException(
                MavenMessageCode.PARAMETER_CONTAINS_INVALID, "version", ILLEGAL_VERSION_CHARS
            )
        }
        this.groupId?.let { groupId ->
            if (!validateId(groupId)) throw ErrorCodeException(
                MavenMessageCode.PARAMETER_EXPECT, "groupId", ValidId_CHARS
            )
        }
        this.groupId?.let { artifactId ->
            if (!validateId(artifactId)) throw ErrorCodeException(
                MavenMessageCode.PARAMETER_EXPECT, "artifactId", ValidId_CHARS
            )
        }
    }
}
