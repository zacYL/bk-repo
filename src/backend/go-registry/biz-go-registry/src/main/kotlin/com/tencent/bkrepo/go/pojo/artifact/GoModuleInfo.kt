/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
 *
 * Copyright (C) 2024 THL A29 Limited, a Tencent company.  All rights reserved.
 *
 * BK-CI 蓝鲸持续集成平台 is licensed under the MIT license.
 *
 * A copy of the MIT License is included in this file.
 *
 *
 * Terms of the MIT License:
 * ---------------------------------------------------
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
 * documentation files (the "Software"), to deal in the Software without restriction, including without limitation the
 * rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to
 * permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of
 * the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT
 * LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN
 * NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
 * SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package com.tencent.bkrepo.go.pojo.artifact

import com.tencent.bkrepo.common.api.constant.StringPool
import com.tencent.bkrepo.go.constant.GENERIC_MAJOR_VERSION_SUFFIX_REGEX
import com.tencent.bkrepo.go.constant.GOPKG_IN
import com.tencent.bkrepo.go.constant.GOPKG_MAJOR_VERSION_SUFFIX_REGEX
import com.tencent.bkrepo.go.constant.INCOMPATIBLE_SUFFIX
import com.tencent.bkrepo.go.pojo.enum.GoFileType
import com.tencent.bkrepo.go.util.GoUtils.caseEncode

/**
 * go module构件信息
 */
class GoModuleInfo(
    projectId: String,
    repoName: String,
    modulePath: String,
    override val version: String,
    val type: GoFileType
) : GoArtifactInfo(projectId, repoName, modulePath, version) {

    private val name = getMajorVersionSuffix()
        ?.let { modulePath.removeSuffix(it).substringAfterLast(StringPool.SLASH) }
        ?: modulePath.substringAfterLast(StringPool.SLASH)

    override fun getArtifactFullPath() = getArtifactFullPathByType(type)

    override fun getArtifactName() = name

    override fun getArtifactVersion(): String = version

    override fun getRequestPath() = "/${modulePath.caseEncode()}/@v/$version.${type.extension}"

    fun getArtifactFullPathByType(type: GoFileType) = "${getArtifactRootPath()}/$version.${type.extension}"

    private fun getMajorVersionSuffix(): String? {
        if (version.endsWith(INCOMPATIBLE_SUFFIX)) return null
        val sourceGopkg = modulePath.startsWith("$GOPKG_IN/")
        val delimiterIndex =
            if (sourceGopkg) modulePath.lastIndexOf(StringPool.DOT) else modulePath.lastIndexOf(StringPool.SLASH)
        if (delimiterIndex < 1) return null
        val modulePathSuffix = modulePath.substring(delimiterIndex)
        val versionPrefix = version.substringBefore(StringPool.DOT)
        val regex = if (sourceGopkg) gopkgMajorVersionSuffixRegex else genericMajorVersionSuffixRegex
        return if (
            regex.matchEntire(modulePathSuffix) == null ||
            modulePathSuffix.substring(1) != versionPrefix
        ) null else modulePathSuffix
    }

    companion object {
        private val genericMajorVersionSuffixRegex = Regex(GENERIC_MAJOR_VERSION_SUFFIX_REGEX)
        private val gopkgMajorVersionSuffixRegex = Regex(GOPKG_MAJOR_VERSION_SUFFIX_REGEX)
    }
}
