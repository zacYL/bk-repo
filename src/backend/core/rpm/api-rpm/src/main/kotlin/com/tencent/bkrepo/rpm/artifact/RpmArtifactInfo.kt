/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
 *
 * Copyright (C) 2020 Tencent.  All rights reserved.
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

package com.tencent.bkrepo.rpm.artifact

import com.tencent.bkrepo.common.artifact.api.ArtifactInfo
import com.tencent.bkrepo.common.artifact.util.PackageKeys
import com.tencent.bkrepo.common.service.util.HttpContextHolder
import org.apache.commons.lang3.StringUtils

class RpmArtifactInfo(
    projectId: String,
    repoName: String,
    artifactUri: String
) : ArtifactInfo(projectId, repoName, artifactUri) {
    companion object {
        const val RPM = "/{projectId}/{repoName}/**"
        const val RPM_CONFIGURATION = "/configuration/{projectId}/{repoName}/**"
        const val RPM_DEBUG_FLUSH = "/flush/{projectId}/{repoName}/**"
        const val RPM_DEBUG_ALL_FLUSH = "/flushAll/{projectId}/{repoName}/"
        const val RPM_FIX_PRIMARY_XML = "/fixPrimaryXml/{projectId}/{repoName}/**"
        const val RPM_EXT_LIST = "/list/{projectId}/{repoName}/**"

        // RPM 产品接口
        const val RPM_EXT_DETAIL = "/version/detail/{projectId}/{repoName}"
        const val RPM_EXT_PACKAGE_DELETE = "/package/delete/{projectId}/{repoName}"
        const val RPM_EXT_VERSION_DELETE = "/version/delete/{projectId}/{repoName}"
    }

    override fun getArtifactFullPath(): String {
        return if(getArtifactMappingUri().isNullOrEmpty()) {
            val action = HttpContextHolder.getRequest().method
            if (action.equals("delete", ignoreCase = true)) {
                val packageKey = HttpContextHolder.getRequest().getParameter("packageKey")
                val version = HttpContextHolder.getRequest().getParameter("version")
                if (StringUtils.isBlank(packageKey)) {
                    super.getArtifactFullPath()
                } else {
                    "/${PackageKeys.resolveRpm(packageKey)}-$version.rpm"
                }
            } else {
                super.getArtifactFullPath()
            }
        } else getArtifactMappingUri()!!
    }
}
