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

package com.tencent.bkrepo.npm.artifact

import com.tencent.bkrepo.npm.constants.DELIMITER_HYPHEN
import com.tencent.bkrepo.npm.constants.FILE_SUFFIX
import com.tencent.bkrepo.npm.constants.HAR_FILE_EXT
import com.tencent.bkrepo.npm.constants.TARBALL_FULL_PATH_FORMAT

class NpmTarballArtifactInfo(
    projectId: String,
    repoName: String,
    packageName: String,
    version: String? = null,
    private val delimiter: String = DELIMITER_HYPHEN,
    private val repeatedScope: Boolean = true,
    private val ohpm: Boolean = false,
) : NpmArtifactInfo(projectId, repoName, packageName, version) {

    override fun getArtifactFullPath(): String {
        require(version != null)
        return getTarballFullPath(packageName, version, delimiter, repeatedScope,ohpm)
    }

    private fun getTarballFullPath(name: String, version: String, delimiter: String, repeatedScope: Boolean, ohpm: Boolean) =
        TARBALL_FULL_PATH_FORMAT.format(
            name, delimiter, if (repeatedScope) name else name.substringAfterLast("/"), version, getContentFileExt(ohpm)
        )

    private fun getContentFileExt(ohpm: Boolean) = if (ohpm) {
        HAR_FILE_EXT
    } else {
        FILE_SUFFIX
    }
}
