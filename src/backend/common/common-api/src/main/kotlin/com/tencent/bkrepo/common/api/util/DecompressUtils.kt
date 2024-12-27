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

package com.tencent.bkrepo.common.api.util

import com.tencent.bkrepo.common.api.exception.NotFoundException
import com.tencent.bkrepo.common.api.message.CommonMessageCode
import org.apache.commons.compress.archivers.ArchiveEntry
import org.apache.commons.compress.archivers.ArchiveException
import org.apache.commons.compress.archivers.ArchiveInputStream
import org.apache.commons.compress.archivers.ArchiveStreamFactory
import org.apache.commons.compress.compressors.CompressorStreamFactory
import java.io.BufferedInputStream
import java.io.InputStream

object DecompressUtils {
    private const val BUFFER_SIZE = 2048

    /**
     * 获取压缩流中的[fileName]文件内容
     */
    fun <E : ArchiveEntry> getContent(archiveInputStream: ArchiveInputStream<E>, fileName: String): String {
        var zipEntry: E
        archiveInputStream.use { archiveEntry ->
            while (archiveInputStream.nextEntry.also { zipEntry = it } != null) {
                if ((!zipEntry.isDirectory) && zipEntry.name.split("/").last() == fileName) {
                    return streamToString(archiveEntry)
                }
            }
        }
        throw NotFoundException(CommonMessageCode.RESOURCE_NOT_FOUND, "Can not find $fileName")
    }

    /**
     * 查找第一个符合其中任意后缀的文件
     * @return 文件名，文件内容
     */
    fun <E : ArchiveEntry> getContentByExtensions(
        archiveInputStream: ArchiveInputStream<E>,
        extensions: List<String>,
    ): Pair<String, String> {
        var zipEntry: E
        archiveInputStream.use { archiveEntry ->
            while (archiveInputStream.nextEntry.also { zipEntry = it } != null) {
                if (!zipEntry.isDirectory) {
                    val fileName = zipEntry.name.split("/").last()
                    val fileExtension = fileName.substringAfterLast('.', "")
                    if (fileExtension in extensions) {
                        return fileName to streamToString(archiveEntry)
                    }
                }
            }
        }
        throw NotFoundException(CommonMessageCode.RESOURCE_NOT_FOUND, "No file found with extensions: $extensions")
    }

    private fun streamToString(inputStream: InputStream): String {
        val stringBuilder = StringBuffer("")
        var length: Int
        val bytes = ByteArray(BUFFER_SIZE)
        while ((inputStream.read(bytes).also { length = it }) != -1) {
            stringBuilder.append(String(bytes, 0, length))
        }
        return stringBuilder.toString()
    }

    /**
     * 使用归档处理器处理输入流中的数据
     *
     * 此函数通过读取输入流中的归档数据，并根据提供的回调函数对每个归档条目进行处理
     * 它支持通过多次尝试来解压缩数据，以便更好地处理复杂或嵌套的归档文件
     *
     * @param `is` 输入流，包含归档数据
     * @param collection 用于收集处理结果的集合
     * @param compressCount 尝试解压缩的次数，默认为2次
     * @param callbackPre 在处理每个归档条目之前调用的回调函数，返回true以继续处理
     * @param callback 处理每个归档条目的回调函数，返回处理结果
     * @param callbackPost 在处理每个归档条目之后调用的回调函数，返回false可提前结束处理
     * @return 处理结果的集合
     */
    fun <R> doWithArchiver(
        `is`: InputStream,
        collection: MutableList<R> = mutableListOf(),
        compressCount: Int = 2,
        callbackPre: (ArchiveEntry) -> Boolean = { true },
        callback: (ArchiveInputStream<*>, ArchiveEntry) -> R,
        callbackPost: (ArchiveEntry, R?) -> Boolean = { _, _ -> true }
    ): List<R> {
        require(compressCount > 1)
        // 根据输入流的支持情况，决定是否需要包装为BufferedInputStream
        var wrap = if (`is`.markSupported()) `is` else BufferedInputStream(`is`)

        // 尝试对输入流进行解压缩处理，最多尝试compressCount次
        for (count in 1..compressCount) {
            try {
                // 尝试使用当前包装的输入流进行归档处理
                return doWithArchiver(wrap, collection, callbackPre, callback, callbackPost)
            } catch (e: ArchiveException) {
                // 如果达到最大尝试次数，则重新抛出异常
                if (count == compressCount) {
                    throw e
                }
                // 如果解压缩失败，尝试使用CompressorStreamFactory创建新的压缩输入流进行下一次尝试
                wrap = BufferedInputStream(CompressorStreamFactory().createCompressorInputStream(wrap))
            }
        }
        return collection
    }

    /**
     * 使用归档处理器处理输入流中的数据
     *
     * 此重载版本的函数提供了一个默认的空集合作为结果收集器，并且允许回调函数返回可空类型
     * 它适用于需要处理可嵌套归档，但不需要多次解压缩尝试的场景
     *
     * @param `is` 输入流，包含归档数据
     * @param collection 用于收集处理结果的集合，默认为mutableListOf()
     * @param callbackPre 在处理每个归档条目之前调用的回调函数，返回true以继续处理
     * @param callback 处理每个归档条目的回调函数，返回处理结果，可以为null
     * @param callbackPost 在处理每个归档条目之后调用的回调函数，返回false可提前结束处理
     * @return 处理结果的集合
     */
    fun <R> doWithArchiver(
        `is`: InputStream,
        collection: MutableList<R> = mutableListOf(),
        callbackPre: (ArchiveEntry) -> Boolean = { true },
        callback: (ArchiveInputStream<*>, ArchiveEntry) -> R?,
        callbackPost: (ArchiveEntry, R?) -> Boolean = { _, _ -> true }
    ): List<R> {
        val wrap = if (`is`.markSupported()) `is` else BufferedInputStream(`is`)
        ArchiveStreamFactory().createArchiveInputStream<ArchiveInputStream<*>>(wrap).use {
            while (true) {
                val entry = it.nextEntry ?: break
                if (!it.canReadEntryData(entry)) continue
                if (!callbackPre(entry)) continue
                val r = callback(it, entry)?.apply { collection.add(this) }
                if (!callbackPost(entry, r)) break
            }
        }
        return collection
    }

}
