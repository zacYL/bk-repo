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

package com.tencent.bkrepo.oci.artifact.repository

import com.tencent.bkrepo.common.api.constant.CharPool.SLASH
import com.tencent.bkrepo.common.artifact.repository.context.ArtifactContext
import com.tencent.bkrepo.common.artifact.repository.context.ArtifactQueryContext
import com.tencent.bkrepo.common.artifact.repository.virtual.VirtualRepository
import com.tencent.bkrepo.oci.constant.LAST_TAG
import com.tencent.bkrepo.oci.constant.N
import com.tencent.bkrepo.oci.pojo.artifact.OciArtifactInfo
import com.tencent.bkrepo.oci.pojo.artifact.OciBlobArtifactInfo
import com.tencent.bkrepo.oci.pojo.artifact.OciManifestArtifactInfo
import com.tencent.bkrepo.oci.pojo.artifact.OciTagArtifactInfo
import com.tencent.bkrepo.oci.pojo.response.CatalogResponse
import com.tencent.bkrepo.oci.pojo.tags.TagsInfo
import com.tencent.bkrepo.oci.util.OciUtils
import com.tencent.bkrepo.repository.pojo.repo.RepositoryDetail
import org.springframework.stereotype.Component

@Component
class OciRegistryVirtualRepository : VirtualRepository() {

    override fun query(context: ArtifactQueryContext): Any? {
        val artifactInfo = context.artifactInfo
        return if (artifactInfo is OciTagArtifactInfo) {
            val n = context.getAttribute<Int>(N)
            val last = context.getAttribute<String>(LAST_TAG)
            if (artifactInfo.packageName.isBlank()) {
                val packageList = mapEachLocal(context) { sub, repository ->
                    require(sub is ArtifactQueryContext)
                    sub.getAndRemoveAttribute<Int>(N)
                    sub.getAndRemoveAttribute<String>(LAST_TAG)
                    repository.query(sub) as CatalogResponse
                }.flatMap { it.repositories }.toSortedSet().toMutableList()
                val (imageList, left) = OciUtils.filterHandler(
                    tags = packageList,
                    n = n,
                    last = last
                )
                CatalogResponse(imageList, left)
            } else {
                val tagList = mapEachLocalAndFirstRemote(context) { sub, repository ->
                    require(sub is ArtifactQueryContext)
                    sub.getAndRemoveAttribute<Int>(N)
                    sub.getAndRemoveAttribute<String>(LAST_TAG)
                    repository.query(sub) as TagsInfo
                }.flatMap { it.tags }.toSortedSet().toMutableList()
                val pair = OciUtils.filterHandler(
                    tags = tagList,
                    n = n,
                    last = last
                )
                TagsInfo(artifactInfo.packageName, pair.first as List<String>, pair.second)
            }
        } else {
            mapFirstRepo(context) { sub, repository ->
                require(sub is ArtifactQueryContext)
                repository.query(sub)
            }
        }
    }

    override fun generateSubContext(context: ArtifactContext, subRepoDetail: RepositoryDetail): ArtifactContext {
        with(context.artifactInfo as OciArtifactInfo) {
            val attrMap = if (
                (this is OciManifestArtifactInfo || this is OciBlobArtifactInfo) && packageName.contains(SLASH)
            ) {
                OciUtils.getDefaultNamespace(subRepoDetail.configuration)
                    ?.let { mapOf(this::packageName.name to packageName.removePrefix("$it/")) }
            } else null
            return context.copy(subRepoDetail, attrMap, null)
        }
    }
}
