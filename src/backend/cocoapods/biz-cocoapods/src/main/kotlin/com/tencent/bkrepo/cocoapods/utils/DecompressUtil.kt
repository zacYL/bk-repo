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

package com.tencent.bkrepo.cocoapods.utils

import com.tencent.bkrepo.cocoapods.exception.CocoapodsMessageCode
import com.tencent.bkrepo.cocoapods.exception.CocoapodsPodSpecNotFoundException
import com.tencent.bkrepo.cocoapods.pojo.PodSpec
import com.tencent.bkrepo.cocoapods.pojo.enums.PodSpecType
import com.tencent.bkrepo.common.api.util.DecompressUtils
import org.apache.commons.compress.archivers.tar.TarArchiveEntry
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream
import java.io.InputStream
import java.util.zip.GZIPOutputStream
import javax.servlet.http.HttpServletResponse

object DecompressUtil {

    private fun getContentByExtensionsFromTarGz(
        ips: InputStream,
        extensions: List<String>,
    ): Pair<String, String> {
        ips.use { fileInputStream ->
            GzipCompressorInputStream(fileInputStream).use { gzipInputStream ->
                TarArchiveInputStream(gzipInputStream).use { tarInputStream ->
                    return DecompressUtils.getContentByExtensions(tarInputStream, extensions)
                }
            }
        }
    }

    /**
     * 从压缩包中获取podspec文件，并更新源
     */
    fun InputStream.getPodSpec(cachePath: String): PodSpec {
        val (fileName, content) = getContentByExtensionsFromTarGz(this, PodSpecType.extendedNames())
        val type = PodSpecType.matchPath(fileName)
            ?: throw CocoapodsPodSpecNotFoundException(CocoapodsMessageCode.COCOAPODS_PODSPEC_NOT_FOUND)

        val specName: String?
        val podSpecContent = when (type) {
            PodSpecType.POD_SPEC -> {
                specName = CocoapodsUtil.extractNameFromPodspec(content)
                CocoapodsUtil.updatePodspecSource(content, cachePath)
            }

            PodSpecType.JSON -> {
                specName = CocoapodsUtil.extractNameFromPodspecJson(content)
                CocoapodsUtil.updatePodspecJsonSource(content, cachePath)
            }
        }
        return PodSpec(name = specName, fileName = fileName, content = podSpecContent)
    }

    fun buildEmptySpecGzOps(response: HttpServletResponse): GZIPOutputStream {
        val gzipOutputStream = GZIPOutputStream(response.outputStream)
        TarArchiveOutputStream(gzipOutputStream).use { tarOutputStream ->

            // 添加一个空目录 "Specs"
            val entry: TarArchiveEntry = TarArchiveEntry("Specs/")
            entry.setSize(0)
            tarOutputStream.putArchiveEntry(entry)
            tarOutputStream.closeArchiveEntry()
        }
        return gzipOutputStream
    }
}
