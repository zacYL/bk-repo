/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
 *
 * Copyright (C) 2020 THL A29 Limited, a Tencent company.  All rights reserved.
 *
 * BK-CI 蓝鲸持续集成平台 is licensed under the MIT license.
 *
 * A copy of the MIT License is included in this file.
 *
 *
 * Terms of the MIT License:
 * ---------------------------------------------------
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package com.tencent.bkrepo.ivy.artifact

import com.tencent.bkrepo.common.api.constant.CharPool
import com.tencent.bkrepo.common.artifact.api.ArtifactFile
import com.tencent.bkrepo.common.artifact.api.ArtifactInfo
import com.tencent.bkrepo.ivy.enum.HashType
import com.tencent.bkrepo.repository.pojo.repo.RepositoryDetail
import java.io.File
import java.io.InputStream
import javax.xml.parsers.DocumentBuilderFactory

class IvyArtifactInfo(
    projectId: String,
    repoName: String,
    artifactUri: String
) : ArtifactInfo(projectId, repoName, artifactUri) {

    companion object {
        const val IVY_MAPPING_URI = "/{projectId}/{repoName}/**"
        const val ARTIFACT_PATTERN_KEY = "artifact_pattern"
        const val IVY_PATTERN_KEY = "ivy_pattern"
    }

    fun isSummaryFile(): Boolean {
        return HashType.values().any() { getArtifactFullPath().endsWith(".${it.ext}") }
    }

    fun getExt(): String {
        return getArtifactFullPath().substringAfterLast(CharPool.DOT)
    }

    fun isIvyXml(inputStream: InputStream): Boolean {
        val dbFactory = DocumentBuilderFactory.newInstance()
        val dBuilder = dbFactory.newDocumentBuilder()
        val doc = dBuilder.parse(inputStream)
        doc.documentElement.normalize()

        val rootElement = doc.documentElement
        return "ivy-module" == rootElement.nodeName
    }

    fun getRepoArtifactPattern(repositoryDetail: RepositoryDetail): String {
        return repositoryDetail.configuration.getStringSetting(ARTIFACT_PATTERN_KEY) ?: ""
    }

    fun getRepoIvyPattern(repositoryDetail: RepositoryDetail): String {
        return repositoryDetail.configuration.getStringSetting(IVY_PATTERN_KEY) ?: ""
    }

    fun getFile(artifactFile: ArtifactFile): File {
        artifactFile.apply {
            if (this.getFile() == null) this.flushToFile()
        }
        return artifactFile.getFile()!!
    }

    // 解析摘要文件获取制品文件路径
    fun extractArtifactFilePathFromSummary(): String {
        return if (isSummaryFile()) {
            getArtifactFullPath().substringBeforeLast(CharPool.DOT)
        } else {
            ""
        }
    }

    override fun getPackageFullName() = ""

    override fun getArtifactVersion() = ""
}
