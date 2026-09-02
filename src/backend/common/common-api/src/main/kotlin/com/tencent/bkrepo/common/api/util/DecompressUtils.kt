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

package com.tencent.bkrepo.common.api.util

import com.tencent.bkrepo.common.api.exception.ErrorCodeException
import com.tencent.bkrepo.common.api.exception.NotFoundException
import com.tencent.bkrepo.common.api.message.CommonMessageCode
import org.apache.commons.compress.archivers.ArchiveEntry
import org.apache.commons.compress.archivers.ArchiveInputStream
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.nio.charset.StandardCharsets

object DecompressUtils {
    private const val BUFFER_SIZE = 2048
    const val DEFAULT_MAX_METADATA_SIZE = 10 * 1024 * 1024

    /**
     * 获取压缩流中的[fileName]文件内容
     */
    fun getContent(
        archiveInputStream: ArchiveInputStream,
        fileName: String,
        maxSize: Int = DEFAULT_MAX_METADATA_SIZE
    ): String {
        var zipEntry: ArchiveEntry
        archiveInputStream.use { it ->
            while (archiveInputStream.nextEntry.also { zipEntry = it } != null) {
                if ((!zipEntry.isDirectory) && zipEntry.name.split("/").last() == fileName) {
                    return streamToString(it, maxSize)
                }
            }
        }
        throw NotFoundException(CommonMessageCode.RESOURCE_NOT_FOUND, "Can not find $fileName")
    }

    fun streamToString(inputStream: InputStream, maxSize: Int = DEFAULT_MAX_METADATA_SIZE): String {
        val output = ByteArrayOutputStream()
        val bytes = ByteArray(BUFFER_SIZE)
        var total = 0
        var length: Int
        while ((inputStream.read(bytes).also { length = it }) != -1) {
            total += length
            if (total > maxSize) {
                throw ErrorCodeException(CommonMessageCode.REQUEST_CONTENT_INVALID)
            }
            output.write(bytes, 0, length)
        }
        return output.toString(StandardCharsets.UTF_8)
    }
}
