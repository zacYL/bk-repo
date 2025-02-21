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

package com.tencent.bkrepo.replication.replica.repository.internal.type

import com.tencent.bkrepo.common.artifact.pojo.RepositoryType
import com.tencent.bkrepo.common.metadata.service.node.NodeService
import com.tencent.bkrepo.common.metadata.util.PackageKeys
import com.tencent.bkrepo.repository.pojo.packages.PackageSummary
import com.tencent.bkrepo.repository.pojo.packages.PackageVersion
import org.springframework.stereotype.Component

@Component
class GoPackageNodeMapper(
    private val nodeService: NodeService
) : PackageNodeMapper {

    override fun type() = RepositoryType.GO

    override fun extraType(): RepositoryType? {
        return null
    }

    override fun map(
        packageSummary: PackageSummary,
        packageVersion: PackageVersion,
        type: RepositoryType
    ): List<String> {
        val modulePath = PackageKeys.resolveGo(packageSummary.key)
        val version = packageVersion.name
        val modFullPath = getGoModuleFullPath(modulePath, version, "mod")
        val readmeFullPath = GO_MODULE_ROOT_PATH.format(modulePath, version) + "/.readme"
        return listOf(getGoModuleFullPath(modulePath, version, "zip")) + nodeService.listExistFullPath(
            packageSummary.projectId, packageSummary.repoName, listOf(modFullPath, readmeFullPath)
        )
    }

    private fun getGoModuleFullPath(modulePath: String, version: String, extension: String) =
        GO_MODULE_FULL_PATH.format(modulePath, version, version, extension)

    companion object {
        const val GO_MODULE_ROOT_PATH = "/%s/@v/%s"
        const val GO_MODULE_FULL_PATH = "$GO_MODULE_ROOT_PATH/%s.%s"
    }
}
