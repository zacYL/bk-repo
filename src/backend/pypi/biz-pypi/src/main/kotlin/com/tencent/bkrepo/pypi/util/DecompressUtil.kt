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

package com.tencent.bkrepo.pypi.util

import com.tencent.bkrepo.common.api.exception.NotFoundException
import com.tencent.bkrepo.common.api.message.CommonMessageCode
import org.apache.commons.compress.archivers.ArchiveInputStream
import org.apache.commons.compress.archivers.zip.ZipArchiveInputStream
import java.io.InputStream
import org.apache.commons.compress.archivers.ArchiveEntry
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream
import java.util.zip.GZIPInputStream

object DecompressUtil {

    private const val BUFFER_SIZE = 2048
    private const val METADATA = "METADATA"
    private const val PKG_INFO = "PKG-INFO"
    private const val DIST_INFO = ".dist-info"
    private const val TAR = "tar"
    private const val ZIP = "zip"
    private const val WHL = "whl"
    private const val TAR_GZ = "tar.gz"
    private const val TGZ = "tgz"

    @Throws(Exception::class)
    fun InputStream.getPypiMetadata(fileName: String): String {
        return when (getFormat(fileName)) {
            TAR -> {
                getTarArchiversContent(this)
            }
            ZIP -> {
                getZipArchiversContent(this)
            }
            WHL -> {
                getZipArchiversContent(this, true)
            }
            TAR_GZ,
            TGZ -> {
                getTgzArchiversContent(this)
            }
            else -> {
                "can not support compress format!"
            }
        }
    }

    @Throws(Exception::class)
    fun getZipArchiversContent(inputStream: InputStream, whlFormat: Boolean = false): String {
        return getArchiversContent(ZipArchiveInputStream(inputStream), whlFormat)
    }

    @Throws(Exception::class)
    fun getTgzArchiversContent(inputStream: InputStream): String {
        return getArchiversContent(TarArchiveInputStream(GZIPInputStream(inputStream)))
    }

    @Throws(Exception::class)
    fun getTarArchiversContent(inputStream: InputStream): String {
        return getArchiversContent(TarArchiveInputStream(inputStream))
    }

    private fun <E : ArchiveEntry> getArchiversContent(
        archiveInputStream: ArchiveInputStream<E>,
        whlFormat: Boolean = false
    ): String {
        var zipEntry: E
        archiveInputStream.use { it ->
            while (it.nextEntry.also { zipEntry = it } != null) {
                if (isMatchEntry(zipEntry, whlFormat)) {
                    return parseStream(it)
                }
            }
        }
        throw NotFoundException(CommonMessageCode.RESOURCE_NOT_FOUND, "Can not find metadata file")
    }

    private fun <E : ArchiveEntry> parseStream(archiveInputStream: ArchiveInputStream<E>): String {
        val stringBuilder = StringBuffer()
        var length: Int
        val bytes = ByteArray(BUFFER_SIZE)
        while ((archiveInputStream.read(bytes).also { length = it }) != -1) {
            stringBuilder.append(String(bytes, 0, length))
        }
        return stringBuilder.toString()
    }

    private fun isMatchEntry(zipEntry: ArchiveEntry, whlFormat: Boolean): Boolean {
        if (zipEntry.isDirectory) return false
        val entryList = zipEntry.name.split("/")
        return if (whlFormat) {
            entryList.last() == METADATA && entryList.getOrNull(entryList.size - 2)?.endsWith(DIST_INFO) ?: false
        } else {
            entryList.size <= 2 && entryList.last() == PKG_INFO
        }
    }

    private fun getFormat(fileName: String): String {
        return if (fileName.endsWith(".zip")) {
            ZIP
        } else if (fileName.endsWith(".tar")) {
            TAR
        } else if (fileName.endsWith(".tar.gz")) {
            TAR_GZ
        } else if (fileName.endsWith(".tgz")) {
            TGZ
        } else if (fileName.endsWith(".whl")) {
            WHL
        } else {
            "can not support compress format!"
        }
    }
}
