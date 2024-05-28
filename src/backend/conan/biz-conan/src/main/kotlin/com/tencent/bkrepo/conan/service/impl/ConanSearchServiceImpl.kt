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

package com.tencent.bkrepo.conan.service.impl

import com.tencent.bkrepo.common.artifact.api.ArtifactInfo
import com.tencent.bkrepo.common.artifact.exception.NodeNotFoundException
import com.tencent.bkrepo.common.artifact.repository.context.ArtifactContextHolder
import com.tencent.bkrepo.common.artifact.repository.context.ArtifactQueryContext
import com.tencent.bkrepo.conan.constant.ConanMessageCode
import com.tencent.bkrepo.conan.constant.IGNORECASE
import com.tencent.bkrepo.conan.constant.PATTERN
import com.tencent.bkrepo.conan.exception.ConanSearchNotFoundException
import com.tencent.bkrepo.conan.pojo.ConanInfo
import com.tencent.bkrepo.conan.pojo.ConanSearchResult
import com.tencent.bkrepo.conan.pojo.artifact.ConanArtifactInfo
import com.tencent.bkrepo.conan.service.ConanSearchService
import com.tencent.bkrepo.conan.utils.ConanArtifactInfoUtil.convertToConanFileReference
import com.tencent.bkrepo.conan.utils.PathUtils.buildReference
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class ConanSearchServiceImpl : ConanSearchService {

    @Autowired
    lateinit var commonService: CommonService

    override fun search(
        artifactInfo: ArtifactInfo,
        pattern: String?,
        ignoreCase: Boolean
    ): ConanSearchResult {
        val context = ArtifactQueryContext(artifact = artifactInfo)
        pattern?.let { context.putAttribute(PATTERN, it) }
        context.putAttribute(IGNORECASE, ignoreCase)
        return ArtifactContextHolder.getRepository().query(context) as ConanSearchResult
    }

    override fun searchPackages(pattern: String?, conanArtifactInfo: ConanArtifactInfo): Map<String, ConanInfo> {
        with(conanArtifactInfo) {
            val conanFileReference = convertToConanFileReference(conanArtifactInfo)
            val result = try {
                commonService.getPackageConanInfo(projectId, repoName, conanFileReference)
            } catch (ignore: NodeNotFoundException) {
                emptyMap()
            }
            if (result.isEmpty()) {
                throw ConanSearchNotFoundException(
                    ConanMessageCode.CONAN_SEARCH_NOT_FOUND, buildReference(conanFileReference), getRepoIdentify()
                )
            }
            return result
        }
    }

    override fun searchRevision(conanArtifactInfo: ConanArtifactInfo): Map<String,ConanInfo> {
        with(conanArtifactInfo) {
            val conanFileReference = convertToConanFileReference(conanArtifactInfo)
            val result = try {
                commonService.getPackageConanInfoByRevision(projectId, repoName,conanArtifactInfo.revision!!, conanFileReference)
            } catch (ignore: NodeNotFoundException) {
                emptyMap()
            }
            if (result.isEmpty()) {
                throw ConanSearchNotFoundException(
                    ConanMessageCode.CONAN_SEARCH_NOT_FOUND, buildReference(conanFileReference), getRepoIdentify()
                )
            }
            return result
        }
    }
}
