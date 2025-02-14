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

package com.tencent.bkrepo.go.artifact.resolver

import com.tencent.bkrepo.common.api.constant.CharPool
import com.tencent.bkrepo.common.api.constant.StringPool
import com.tencent.bkrepo.common.artifact.api.ArtifactInfo
import com.tencent.bkrepo.common.artifact.resolve.path.ArtifactInfoResolver
import com.tencent.bkrepo.common.artifact.resolve.path.Resolver
import com.tencent.bkrepo.go.constant.CANONICAL_VERSION
import com.tencent.bkrepo.go.constant.EXTENSION
import com.tencent.bkrepo.go.constant.GENERIC_CURSORY_PATH_MAJOR
import com.tencent.bkrepo.go.constant.GENERIC_PRECISE_PATH_MAJOR
import com.tencent.bkrepo.go.constant.GOPKG_IN
import com.tencent.bkrepo.go.constant.GOPKG_IN_PATH_MAJOR
import com.tencent.bkrepo.go.constant.INCOMPATIBLE_METADATA
import com.tencent.bkrepo.go.constant.MODULE_PATH_ELEMENT
import com.tencent.bkrepo.go.constant.MODULE_PATH_PREFIX
import com.tencent.bkrepo.go.constant.PATH_DELIMITER
import com.tencent.bkrepo.go.constant.UNSTABLE_PRERELEASE
import com.tencent.bkrepo.go.constant.VERSION
import com.tencent.bkrepo.go.exception.GoInvalidVersionException
import com.tencent.bkrepo.go.exception.MalformedModulePathException
import com.tencent.bkrepo.go.pojo.artifact.GoModuleInfo
import com.tencent.bkrepo.go.pojo.enum.GoFileType
import com.tencent.bkrepo.go.util.GoUtils.caseDecode
import org.slf4j.LoggerFactory
import org.springframework.http.HttpMethod
import org.springframework.stereotype.Component
import org.springframework.web.servlet.HandlerMapping
import javax.servlet.http.HttpServletRequest

@Component
@Resolver(GoModuleInfo::class)
class GoModuleInfoResolver : ArtifactInfoResolver {
    override fun resolve(
        projectId: String,
        repoName: String,
        artifactUri: String,
        request: HttpServletRequest
    ): ArtifactInfo {
        val attributes = request.getAttribute(HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE) as Map<*, *>
        val modulePath = artifactUri.substringBefore(PATH_DELIMITER).trim(CharPool.SLASH).caseDecode()
        val version = attributes[VERSION].toString().trim()
        val extension = attributes[EXTENSION].toString().trim()
        if (request.method == HttpMethod.PUT.name) check(projectId, repoName, modulePath, version)
        val name = getMajorVersionSuffix(modulePath)
            ?.let { modulePath.removeSuffix(it).substringAfterLast(StringPool.SLASH) }
            ?: modulePath.substringAfterLast(StringPool.SLASH)
        return GoModuleInfo(projectId, repoName, modulePath, version, name, GoFileType.valueOf(extension.toUpperCase()))
    }

    private fun getMajorVersionSuffix(modulePath: String): String? {
        return if (modulePath.startsWith("$GOPKG_IN/")) {
            modulePath.substring(modulePath.lastIndexOf(StringPool.DOT))
        } else {
            val lastSlashIndex = modulePath.lastIndexOf(StringPool.SLASH).takeIf { it > 0 } ?: return null
            val modulePathSuffix = modulePath.substring(lastSlashIndex)
            if (genericCursoryPathMajorRegex.matchEntire(modulePathSuffix) != null) modulePathSuffix else null
        }
    }

    /**
     * The lexical restrictions of module path:
     * 1. The path must consist of one or more path elements separated by slashes (/, U+002F).
     * It must not begin or end with a slash.
     * 2. Each path element is a non-empty string made of up ASCII letters, ASCII digits,
     * and limited ASCII punctuation (-, ., _, and ~).
     * 3. A path element may not begin or end with a dot (., U+002E).
     * 4. The element prefix up to the first dot must not be a reserved file name on Windows,
     * regardless of case (CON, com1, NuL, and so on).
     * 5. The element prefix up to the first dot must not end with a tilde followed by one or more digits
     * (like EXAMPL~1.COM).
     * 6. The leading path element (up to the first slash, if any), by convention a domain name,
     * must contain only lower-case ASCII letters, ASCII digits, dots (., U+002E), and dashes (-, U+002D);
     * it must contain at least one dot and cannot start with a dash.
     * 7. For a final path element of the form /vN where N looks numeric (ASCII digits and dots),
     * N must not begin with a leading zero, must not be /v1, and must not contain any dots.
     * 7.1. For paths beginning with gopkg.in/, this requirement is replaced by a requirement
     * that the path follow the gopkg.in service’s conventions.
     *
     * https://cs.opensource.google/go/x/mod/+/master:module/module.go
     */
    private fun check(projectId: String, repoName: String, modulePath: String, version: String) {
        // pre-check
        val isValidPath =
            if (modulePath.startsWith("$GOPKG_IN/"))
                modulePath.lastIndexOf(StringPool.DOT) >= GOPKG_IN.length + 2
            else modulePath.lastIndexOf(StringPool.SLASH) >= 3
        if (!isValidPath) throw MalformedModulePathException(projectId, repoName, modulePath)

        // check major version suffix
        val pathMajor = getMajorVersionSuffix(modulePath)
            ?.also { checkMajorVersionSuffix(projectId, repoName, modulePath, it) }

        // check module path
        checkModulePath(projectId, repoName, modulePath, pathMajor)

        // check version
        if (canonicalVersionRegex.matchEntire(version) == null) {
            throw GoInvalidVersionException(projectId, repoName, modulePath, version)
        }
        checkIfPathMajorMatchesVersion(projectId, repoName, modulePath, version, pathMajor)
    }

    private fun checkMajorVersionSuffix(projectId: String, repoName: String, modulePath: String, pathMajor: String) {
        val regex =
            if (modulePath.startsWith("$GOPKG_IN/")) gopkgInPathMajorRegex else genericPrecisePathMajorRegex
        if (regex.matchEntire(pathMajor) == null) {
            logger.warn("Malformed major version suffix [$pathMajor] of [$modulePath]")
            throw MalformedModulePathException(projectId, repoName, modulePath)
        }
    }

    private fun checkModulePath(projectId: String, repoName: String, modulePath: String, pathMajor: String?) {
        val trimmedModulePath = pathMajor?.let { modulePath.removeSuffix(it) } ?: modulePath
        trimmedModulePath.split(StringPool.SLASH).forEachIndexed { index, element ->
            val regex = if (index == 0) modulePathPrefixRegex else modulePathElementRegex
            if (regex.matchEntire(element) == null) {
                logger.warn("Malformed path element [$element] of [$modulePath]")
                throw MalformedModulePathException(projectId, repoName, modulePath)
            }
        }
    }

    private fun checkIfPathMajorMatchesVersion(
        projectId: String,
        repoName: String,
        modulePath: String,
        version: String,
        pathMajor: String?
    ) {
        if (version.startsWith("v0.0.0-") && pathMajor?.removeSuffix(UNSTABLE_PRERELEASE) == ".v1") return
        val versionMajor = version.substringBefore(StringPool.DOT)
        if (pathMajor == null) {
            if (versionMajor == "v0" || versionMajor == "v1" || version.endsWith(INCOMPATIBLE_METADATA)) return
        } else {
            if (pathMajor.substring(1).removeSuffix(UNSTABLE_PRERELEASE) == versionMajor) return
        }
        logger.warn("major version suffix[$pathMajor] of [$modulePath] do not match version[$version]")
        throw GoInvalidVersionException(projectId, repoName, modulePath, version)
    }

    companion object {
        private val logger = LoggerFactory.getLogger(GoModuleInfoResolver::class.java)
        private val modulePathPrefixRegex = Regex(MODULE_PATH_PREFIX)
        private val modulePathElementRegex = Regex(MODULE_PATH_ELEMENT)
        private val genericCursoryPathMajorRegex = Regex(GENERIC_CURSORY_PATH_MAJOR)
        private val genericPrecisePathMajorRegex = Regex(GENERIC_PRECISE_PATH_MAJOR)
        private val gopkgInPathMajorRegex = Regex(GOPKG_IN_PATH_MAJOR)
        private val canonicalVersionRegex = Regex(CANONICAL_VERSION)
    }
}
