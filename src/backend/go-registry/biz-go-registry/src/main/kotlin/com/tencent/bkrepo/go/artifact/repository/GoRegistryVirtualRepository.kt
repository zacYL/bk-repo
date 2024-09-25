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

package com.tencent.bkrepo.go.artifact.repository

import com.tencent.bkrepo.common.artifact.repository.context.ArtifactQueryContext
import com.tencent.bkrepo.common.artifact.repository.virtual.VirtualRepository
import com.tencent.bkrepo.go.pojo.artifact.GoVersionListInfo
import com.tencent.bkrepo.go.pojo.artifact.GoVersionMetadataInfo
import com.tencent.bkrepo.go.pojo.response.GoVersionMetadata
import com.tencent.bkrepo.go.util.GoUtils.convertToSemver
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

@Component
class GoRegistryVirtualRepository : VirtualRepository() {

    @Suppress("UNCHECKED_CAST")
    override fun query(context: ArtifactQueryContext): Any? {
        with(context) {
            getFullPathInterceptors().forEach { it.intercept(projectId, artifactInfo.getArtifactFullPath()) }
        }
        return when (val artifactInfo = context.artifactInfo) {
            is GoVersionListInfo -> {
                (super.query(context) as List<List<String>>).flatten().distinct().sortedBy { it.convertToSemver() }
            }
            is GoVersionMetadataInfo -> {
                if (artifactInfo.getArtifactVersion() == null) {
                    (super.query(context) as List<GoVersionMetadata>).maxByOrNull { it.version.convertToSemver() }
                } else {
                    mapFirstRepo(context) { sub, repository ->
                        require(sub is ArtifactQueryContext)
                        repository.query(context)
                    }
                }
            }
            else -> throw UnsupportedOperationException()
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(GoRegistryVirtualRepository::class.java)
    }
}
