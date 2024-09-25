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

package com.tencent.bkrepo.go.util

import com.tencent.bkrepo.common.api.constant.StringPool
import com.tencent.bkrepo.go.constant.GO_MOD
import java.io.InputStream
import org.apache.commons.compress.archivers.ArchiveEntry
import org.apache.commons.compress.archivers.ArchiveInputStream
import org.apache.commons.compress.archivers.zip.ZipArchiveEntry
import org.apache.commons.compress.archivers.zip.ZipArchiveInputStream

object DecompressUtil {

    private const val BUFFER_SIZE = 2048
    private const val FILE_SIZE_LIMIT = 16 * 1024 * 1024
    private val README_NAME = listOf("readme", "readme.md", "readme.txt")

    @Throws(Exception::class)
    fun InputStream.readModAndReadmeContent(readMod: Boolean = true, readReadme: Boolean = true) =
        this.use { readModAndReadmeContent(ZipArchiveInputStream(this), readMod, readReadme) }

    @Throws(Exception::class)
    fun InputStream.listFile() = listFile(ZipArchiveInputStream(this))

    @Suppress("LoopWithTooManyJumpStatements")
    private fun readModAndReadmeContent(
        archiveInputStream: ArchiveInputStream<ZipArchiveEntry>,
        readMod: Boolean = true,
        readReadme: Boolean = true
    ): Pair<String?, String?> {
        var mod: String? = null
        var readme: String? = null
        var shouldExtractMod = readMod
        var shouldExtractReadme = readReadme
        while (true) {
            val zipEntry = archiveInputStream.nextEntry ?: break
            if (zipEntry.isDirectory || zipEntry.size > FILE_SIZE_LIMIT) continue
            val nameParts = zipEntry.name.split("/")
            if (shouldExtractMod && matchModFile(nameParts)) {
                mod = parseStream(archiveInputStream)
                shouldExtractMod = false
            } else if (shouldExtractReadme && matchReadmeFile(nameParts)) {
                readme = parseStream(archiveInputStream)
                shouldExtractReadme = false
            }
            if (!shouldExtractMod && !shouldExtractReadme) return Pair(mod, readme)
        }
        return Pair(mod, readme)
    }

    @Suppress("LoopWithTooManyJumpStatements")
    private fun listFile(archiveInputStream: ArchiveInputStream<ZipArchiveEntry>): List<String> {
        val list = mutableListOf<String>()
        archiveInputStream.use {
            while (true) {
                val zipEntry = it.nextEntry ?: break
                if (zipEntry.isDirectory) continue
                list.add(zipEntry.name.substringAfterLast(StringPool.SLASH))
            }
        }
        return list
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

    private fun matchModFile(nameParts: List<String>): Boolean {
        return nameParts.last() == GO_MOD &&
                nameParts.getOrNull(nameParts.size - 2)?.contains("@v") ?: false
    }

    private fun matchReadmeFile(nameParts: List<String>): Boolean {
        return if (nameParts.last().toLowerCase() in README_NAME) {
            val parent = nameParts.getOrNull(nameParts.size - 2)
            parent == ".github" || parent?.contains("@v") == true
        } else false
    }
}
