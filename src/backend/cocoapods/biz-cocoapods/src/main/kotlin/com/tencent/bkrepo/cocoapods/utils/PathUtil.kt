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

package com.tencent.bkrepo.cocoapods.utils

import com.tencent.bkrepo.cocoapods.constant.DOT_SPECS
import com.tencent.bkrepo.cocoapods.model.TCocoapodsRemotePackage
import com.tencent.bkrepo.cocoapods.pojo.CocoapodsRemoteConfiguration
import com.tencent.bkrepo.cocoapods.pojo.artifact.CocoapodsArtifactInfo
import com.tencent.bkrepo.cocoapods.pojo.enums.PodSpecSourceType
import com.tencent.bkrepo.cocoapods.pojo.enums.RemoteRepoType
import com.tencent.bkrepo.common.artifact.pojo.configuration.remote.RemoteConfiguration
import com.tencent.bkrepo.common.artifact.repository.context.ArtifactUploadContext
import com.tencent.bkrepo.repository.pojo.packages.PackageVersion

object PathUtil {
    fun generateFullPath(artifactInfo: CocoapodsArtifactInfo): String {
        return with(artifactInfo) {
            "/$orgName/$name/$version/$fileName"
        }
    }

    fun generateSpecsPath(artifactInfo: CocoapodsArtifactInfo): String {
        return with(artifactInfo) {
            ".specs/$name/$version/$name.podspec"
        }
    }

    fun getOrgNameByVersion(packageVersion: PackageVersion): String {
        with(packageVersion) {
            val parts = contentPath?.split("/")?.filter { it.isNotEmpty() }
            return parts?.getOrNull(0)?:""
        }
    }

    fun generateIndexPath(artifactInfo: CocoapodsArtifactInfo, fileName: String?) =
        "$DOT_SPECS/${artifactInfo.name}/${artifactInfo.version}/${fileName}"

    fun generateIndexTarPath() =
        "$DOT_SPECS/index.tar.gz"

    fun ArtifactUploadContext.generateCachePath(artifactInfo: CocoapodsArtifactInfo, domain: String) =
        "${domain}/${projectId}/${repoName}/${artifactInfo.getArtifactFullPath()}"

    fun ArchiveModifier.Podspec.generateCachePath(projectId: String, repoName: String, domain: String) =
        "$domain/$projectId/$repoName/$name/$version/$name-$version.tar.gz}"

    fun buildRemoteSpecsUrl(cocoapodsConf: CocoapodsRemoteConfiguration, conf: RemoteConfiguration) =
        when (cocoapodsConf.type) {
            RemoteRepoType.GIT_HUB -> {
                cocoapodsConf.downloadUrl?:"https://github.com/CocoaPods/Specs.git"
            }

            RemoteRepoType.CPACK -> {
                "${conf.url}/index/fetchIndex"
            }

            RemoteRepoType.OTHER -> {
                cocoapodsConf.downloadUrl ?: kotlin.run {
                    null
                }
            }
        }

    fun TCocoapodsRemotePackage.Source.toDownloadUrl(): String?{
        return when (type) {
            PodSpecSourceType.HTTP.name -> {
                url
            }

            PodSpecSourceType.GIT.name -> {
                "${url.removeSuffix(".git")}/archive/refs/tags/$gitTag.zip"
            }

            else -> {
                null
            }
        }
    }

    fun buildSpecsGitPath(basePath: String, projectId: String, name: String) = "$basePath/$projectId/$name"
}
