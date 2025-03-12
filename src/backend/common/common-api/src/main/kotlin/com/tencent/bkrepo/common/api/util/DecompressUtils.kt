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
import org.apache.commons.compress.compressors.CompressorException
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
                if (zipEntry.isDirectory) continue
                val fileName = zipEntry.name.split("/").last()
                val fileExtension = fileName.substringAfterLast('.', "")
                if (fileExtension in extensions) {
                    return fileName to streamToString(archiveEntry)
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
     * 使用压缩流处理输入流中的归档文件
     *
     * 该函数尝试使用压缩流读取输入流中的归档文件，并对归档中的每个条目执行回调函数
     * 如果输入流不支持标记，则将其包装在BufferedInputStream中以支持标记
     * 如果读取过程中遇到ArchiveException异常，则尝试使用压缩流读取归档文件
     *
     * @param `is` 输入流，用于读取归档文件
     * @param resultFactory 结果工厂，用于创建结果对象的lambda表达式，默认为返回null
     * @param callbackPre 条目预处理回调，用于确定是否处理当前条目的lambda表达式，默认为不处理目录条目
     * @param callback 条目处理回调，用于处理当前条目的lambda表达式，返回处理结果
     * @param handleResult 结果处理回调，用于根据当前条目的处理结果更新总结果的lambda表达式
     * @param callbackPost 条目后处理回调，用于在条目处理后执行额外操作的lambda表达式，默认为空操作
     * @return R? 返回处理结果，如果结果为null，则表示处理失败
     */
    fun <R, E> tryArchiverWithCompressor(
        inputStream: InputStream,
        resultFactory: () -> R? = { null },
        callbackPre: (ArchiveEntry) -> Boolean = { !it.isDirectory },
        callback: (ArchiveInputStream<*>, ArchiveEntry) -> E?,
        handleResult: (R?, E?, ArchiveEntry) -> R?,
        callbackPost: (ArchiveEntry, E?) -> Boolean = { _, _ -> true }
    ): R? {
        // 根据输入流是否支持标记，决定是否将其包装在BufferedInputStream中
        var wrap = if (inputStream.markSupported()) inputStream else BufferedInputStream(inputStream)
        return try {
            // 尝试使用归档流处理输入流
            doWithArchiver(wrap, resultFactory, callbackPre, callback, handleResult, callbackPost)
        } catch (e: ArchiveException) {
            // 如果遇到ArchiveException异常，重置输入流并使用压缩流重新尝试
            wrap.reset()
            wrap = try {
                BufferedInputStream(CompressorStreamFactory().createCompressorInputStream(wrap))
            } catch (e: CompressorException) {
                throw ArchiveException(e.message, e)
            }
            doWithArchiver(wrap, resultFactory, callbackPre, callback, handleResult, callbackPost)
        }
    }

    /**
     * 使用归档流处理输入流中的数据
     *
     * 该函数创建一个ArchiveInputStream并使用它来处理输入流中的归档条目它允许在处理每个条目之前和之后执行自定义操作，
     * 并且可以累积处理结果
     *
     * @param `is` 输入流，包含归档数据
     * @param resultFactory 结果对象的工厂方法，用于创建初始结果对象默认为null
     * @param callbackPre 处理每个归档条目之前调用的回调函数，返回true继续处理，返回false跳过当前条目
     * @param callback 处理每个归档条目的回调函数，返回的结果将传递给handleResult
     * @param handleResult 处理结果的回调函数，用于累积处理结果
     * @param callbackPost 处理每个归档条目之后调用的回调函数，返回false则终止处理
     * @return R? 最终的处理结果，可能为null
     */
    fun <R, E> doWithArchiver(
        inputStream: InputStream,
        resultFactory: () -> R? = { null },
        callbackPre: (ArchiveEntry) -> Boolean = { !it.isDirectory },
        callback: (ArchiveInputStream<*>, ArchiveEntry) -> E?,
        handleResult: (R?, E?, ArchiveEntry) -> R?,
        callbackPost: (ArchiveEntry, E?) -> Boolean = { _, _ -> true }
    ): R? {
        // 初始化结果对象
        var result = resultFactory()
        // 根据输入流是否支持标记来决定是否包装为BufferedInputStream
        val wrap = if (inputStream.markSupported()) inputStream else BufferedInputStream(inputStream)
        // 创建并使用ArchiveInputStream处理归档数据
        ArchiveStreamFactory().createArchiveInputStream<ArchiveInputStream<*>>(wrap).use {
            // 循环处理每个归档条目
            while (true) {
                // 获取下一个归档条目，如果为空则退出循环
                val entry = it.nextEntry ?: break
                // 如果当前条目数据不可读，则跳过
                if (!it.canReadEntryData(entry)) continue
                // 调用预处理回调，决定是否处理当前条目
                if (!callbackPre(entry)) continue
                // 处理当前条目，并获取处理结果
                val r = callback(it, entry)
                // 使用处理结果更新累积结果
                result = handleResult(result, r, entry)
                // 调用后处理回调，决定是否继续处理下一个条目
                if (!callbackPost(entry, r)) break
            }
        }
        // 返回最终的处理结果
        return result
    }

}
