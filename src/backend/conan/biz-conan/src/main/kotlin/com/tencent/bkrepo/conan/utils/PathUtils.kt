/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
 *
 * Copyright (C) 2022 THL A29 Limited, a Tencent company.  All rights reserved.
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

package com.tencent.bkrepo.conan.utils

import com.tencent.bkrepo.common.api.constant.CharPool
import com.tencent.bkrepo.common.api.constant.CharPool.AT
import com.tencent.bkrepo.common.api.constant.CharPool.COLON
import com.tencent.bkrepo.common.api.constant.CharPool.HASH_TAG
import com.tencent.bkrepo.common.api.constant.CharPool.SLASH
import com.tencent.bkrepo.common.api.constant.StringPool.UNDERSCORE
import com.tencent.bkrepo.conan.constant.CONANFILE
import com.tencent.bkrepo.conan.constant.CONANFILE_TXT
import com.tencent.bkrepo.conan.constant.CONANINFO
import com.tencent.bkrepo.conan.constant.CONANS_URL_TAG
import com.tencent.bkrepo.conan.constant.CONAN_V2
import com.tencent.bkrepo.conan.constant.EXPORT_FOLDER
import com.tencent.bkrepo.conan.constant.INDEX_JSON
import com.tencent.bkrepo.conan.constant.PACKAGES_FOLDER
import com.tencent.bkrepo.conan.pojo.ConanFileReference
import com.tencent.bkrepo.conan.pojo.PackageReference
import com.tencent.bkrepo.conan.pojo.artifact.ConanArtifactInfo
import javax.servlet.http.HttpServletRequest

object PathUtils {

    /**
     * url解析为conanFileReference
     * url例子：/{projectId}/{repoName}/v2/conans/{name}/{version}/{username}/{channel}
     */
    fun String.extractConanFileReference(): ConanFileReference {
        val trimStr = this.trim()
        val cleanedUrl = if (trimStr.startsWith(SLASH)) trimStr.substring(1) else trimStr
        val segments = cleanedUrl.split(SLASH)
        if (segments.size < 7 || segments[2] != CONAN_V2 || segments[3] != CONANS_URL_TAG) {
            throw IllegalArgumentException("Invalid URL format")
        }

        val name = segments[4]
        val version = segments[5]
        val userName = segments[6]
        val channel = segments[7]
        return ConanFileReference(name, version, userName, channel)
    }

    fun joinString(first: String, second: String, third: String? = null): String {
        val sb = StringBuilder(first.trimEnd(SLASH))
            .append(SLASH)
            .append(second.trimStart(SLASH))
        third?.let { sb.append(SLASH).append(third) }
        return sb.toString()
    }

    fun buildOriginalConanFileName(fileReference: ConanFileReference): String {
        with(fileReference) {
            return StringBuilder(name)
                .append(SLASH)
                .append(version)
                .append(SLASH)
                .append(userName)
                .append(SLASH)
                .append(channel)
                .toString()
        }
    }

    fun buildConanFileName(fileReference: ConanFileReference): String {
        with(fileReference) {
            return StringBuilder(name)
                .append(SLASH)
                .append(version)
                .append(AT)
                .append(userName)
                .append(SLASH)
                .append(channel)
                .toString()
        }
    }

    fun buildPackagePath(fileReference: ConanFileReference): String {
        with(fileReference) {
            return StringBuilder(userName)
                .append(SLASH)
                .append(name)
                .append(SLASH)
                .append(version)
                .append(SLASH)
                .append(channel)
                .toString()
        }
    }

    fun buildReferenceWithoutVersion(fileReference: ConanFileReference): String {
        with(fileReference) {
            return StringBuilder(userName)
                .append(AT)
                .append(name)
                .append(SLASH)
                .append(channel)
                .toString()
        }
    }
    fun extractNameFromReference(input: String): String {
        return input.substringAfter(AT).substringBefore(SLASH)
    }

    fun buildReference(fileReference: ConanFileReference): String {
        with(fileReference) {
            return StringBuilder(userName)
                .append(SLASH)
                .append(name)
                .append(SLASH)
                .append(version)
                .append(SLASH)
                .append(channel)
                .toString()
        }
    }

    fun buildPackageReference(packageReference: PackageReference): String {
        with(packageReference) {
            return StringBuilder(buildReference(conRef))
                .append(HASH_TAG)
                .append(conRef.revision)
                .append(COLON)
                .append(packageId)
                .toString()
        }
    }

    fun buildRevisionPath(fileReference: ConanFileReference): String {
        with(fileReference) {
            return StringBuilder(buildPackagePath(fileReference))
                .append(SLASH)
                .append(revision)
                .toString()
        }
    }

    fun buildExportFolderPath(fileReference: ConanFileReference): String {
        return StringBuilder(buildRevisionPath(fileReference))
            .append(SLASH)
            .append(EXPORT_FOLDER)
            .toString()
    }

    fun buildPackageFolderPath(fileReference: ConanFileReference): String {
        return StringBuilder(buildRevisionPath(fileReference))
            .append(SLASH)
            .append(PACKAGES_FOLDER)
            .toString()
    }

    fun buildPackageIdFolderPath(fileReference: ConanFileReference, packageId: String): String {
        return StringBuilder(buildRevisionPath(fileReference))
            .append(SLASH)
            .append(PACKAGES_FOLDER)
            .append(SLASH)
            .append(packageId)
            .toString()
    }

    fun buildPackageRevisionFolderPath(packageReference: PackageReference): String {
        with(packageReference) {
            return StringBuilder(buildPackageFolderPath(conRef))
                .append(SLASH)
                .append(packageId)
                .append(SLASH)
                .append(revision)
                .toString()
        }
    }

    fun generateFullPath(artifactInfo: ConanArtifactInfo): String {
        with(artifactInfo) {
            return if (packageId.isNullOrEmpty()) {
                val conanFileReference = ConanArtifactInfoUtil.convertToConanFileReference(this, revision)
                "/${joinString(buildExportFolderPath(conanFileReference), fileName!!)}"
            } else {
                val packageReference = ConanArtifactInfoUtil.convertToPackageReference(this)
                "/${joinString(buildPackageRevisionFolderPath(packageReference), fileName!!)}"
            }
        }
    }

    fun getPackageRevisionsFile(packageReference: PackageReference): String {
        val temp = buildRevisionPath(packageReference.conRef)
        val pFolder = joinString(temp, PACKAGES_FOLDER)
        val pRevison = joinString(pFolder, packageReference.packageId)
        return joinString(pRevison, INDEX_JSON)
    }

    fun getPackageRevisionsFile(conanFileReference: ConanFileReference): String {
        val temp = buildRevisionPath(conanFileReference)
        return joinString(temp, PACKAGES_FOLDER)
    }

    fun getRecipeRevisionsFile(conanFileReference: ConanFileReference): String {
        val recipeFolder = buildPackagePath(conanFileReference)
        return joinString(recipeFolder, INDEX_JSON)
    }

    fun getPackageConanInfoFile(packageReference: PackageReference): String {
        val temp = buildPackageRevisionFolderPath(packageReference)
        return joinString(temp, CONANINFO)
    }

    fun getConanRecipePattern(conanFileReference: ConanFileReference): String {
        with(conanFileReference) {
            //如果userName和channel为_，则不拼接，而是拼接*
            return if (userName == UNDERSCORE && channel == UNDERSCORE) {
                "${name}/${version}*"
            } else {
                buildConanFileName(this)
            }
        }
    }

    fun isSearchPath(uri: String): Boolean {
        return uri.endsWith("/conans/search")

    }

    fun isFirstQueryPath(uri: String): Boolean {
        return isSearchPath(uri) || (uri.endsWith("/latest") && uri.contains("/packages").not())
    }

    fun isConanFile(uri: String): Boolean {
        return uri.endsWith(CONANFILE) || uri.endsWith(CONANFILE_TXT)
    }
}

