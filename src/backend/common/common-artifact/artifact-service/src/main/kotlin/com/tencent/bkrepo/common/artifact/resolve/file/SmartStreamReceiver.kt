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

package com.tencent.bkrepo.common.artifact.resolve.file

import com.tencent.bkrepo.common.artifact.exception.ArtifactReceiveException
import com.tencent.bkrepo.common.artifact.stream.StreamReceiveListener
import com.tencent.bkrepo.common.artifact.stream.rateLimit
import com.tencent.bkrepo.common.artifact.util.http.IOExceptionUtils
import com.tencent.bkrepo.common.storage.core.config.ReceiveProperties
import com.tencent.bkrepo.common.storage.innercos.retry
import com.tencent.bkrepo.common.storage.monitor.StorageHealthMonitor
import com.tencent.bkrepo.common.storage.monitor.Throughput
import com.tencent.bkrepo.common.storage.util.createFile
import org.slf4j.LoggerFactory
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.nio.file.Files
import java.nio.file.Path
import java.security.SecureRandom
import kotlin.math.abs
import kotlin.system.measureNanoTime

class SmartStreamReceiver(
    private val receive: ReceiveProperties,
    private val enableTransfer: Boolean,
    private var path: Path,
    private val filename: String = generateRandomName()
) : StorageHealthMonitor.Observer {
    private val bufferSize = receive.bufferSize.toBytes().toInt()
    private val fileSizeThreshold = receive.fileSizeThreshold.toBytes()
    private val contentBytes = ByteArrayOutputStream(bufferSize)
    private var outputStream: OutputStream = contentBytes
    private var hasTransferred: Boolean = false
    private var fallBackPath: Path? = null

    var isInMemory: Boolean = true
    var totalSize: Long = 0
    var fallback: Boolean = false

    fun receive(source: InputStream, listener: StreamReceiveListener): Throughput {
        try {
            val input = source.rateLimit(receive.rateLimit.toBytes())
            var bytesCopied: Long = 0
            val buffer = ByteArray(bufferSize)
            val nanoTime = measureNanoTime {
                input.use {
                    var bytes = input.read(buffer)
                    while (bytes >= 0) {
                        checkFallback()
                        outputStream.write(buffer, 0, bytes)
                        listener.data(buffer, 0, bytes)
                        bytesCopied += bytes
                        checkThreshold(bytesCopied)
                        bytes = input.read(buffer)
                    }
                }
            }
            totalSize = bytesCopied
            checkSize()
            listener.finished()
            return Throughput(bytesCopied, nanoTime)
        } catch (ignored: IOException) {
            cleanTempFile()
            if (IOExceptionUtils.isClientBroken(ignored)) {
                throw ArtifactReceiveException(ignored.message.orEmpty())
            } else throw ignored
        } finally {
            cleanOriginalOutputStream()
        }
    }

    fun getCachedByteArray(): ByteArray = contentBytes.toByteArray()

    fun getFilePath(): Path = path.resolve(filename)

    @Synchronized
    fun flushToFile(closeStream: Boolean = true) {
        if (isInMemory) {
            val filePath = path.resolve(filename).apply { this.createFile() }
            val fileOutputStream = Files.newOutputStream(filePath)
            contentBytes.writeTo(fileOutputStream)
            outputStream = fileOutputStream
            isInMemory = false

            if (closeStream) {
                cleanOriginalOutputStream()
            }
        }
    }

    override fun unhealthy(fallbackPath: Path?, reason: String?) {
        if (!fallback) {
            fallBackPath = fallbackPath
            fallback = true
            logger.warn("Path[$path] is unhealthy, fallback to use [$fallBackPath], reason: $reason")
        }
    }

    /**
     * 检查是否需要fall back操作
     */
    private fun checkFallback() {
        if (!fallback || hasTransferred) {
            return
        }
        if (fallBackPath == null || fallBackPath == path) {
            logger.info("Fallback path is null or equals to primary path, skip transfer data")
            hasTransferred = true
            return
        }
        // originalPath表示NFS位置， fallBackPath表示本地磁盘位置
        val originalPath = path
        // 更新当前path为本地磁盘
        path = fallBackPath!!
        // transfer date
        if (!isInMemory) {
            // 当文件已经落到NFS
            if (enableTransfer) {
                // 开Transfer功能时，从NFS转移到本地盘
                cleanOriginalOutputStream()
                val originalFile = originalPath.resolve(filename)
                val filePath = path.resolve(filename).apply { this.createFile() }
                originalFile.toFile().inputStream().use {
                    outputStream = filePath.toFile().outputStream()
                    it.copyTo(outputStream, bufferSize)
                }
                Files.deleteIfExists(originalFile)
                logger.info("Success to transfer data from [$originalPath] to [$path]")
            } else {
                // 禁用Transfer功能时，忽略操作，继续使用NFS
                path = originalPath
            }
        }
        hasTransferred = true
    }

    /**
     * 检查是否超出内存阈值，超过则将数据写入文件中，并保持outputStream开启
     */
    private fun checkThreshold(bytesCopied: Long) {
        if (isInMemory && bytesCopied > fileSizeThreshold) {
            flushToFile(false)
        }
    }

    private fun checkSize() {
        if (isInMemory) {
            val actualSize = contentBytes.size().toLong()
            require(totalSize == actualSize) {
                "$totalSize bytes received, but $actualSize bytes saved in memory."
            }
        } else {
            retry(times = RETRY_CHECK_TIMES, delayInSeconds = 1) {
                val actualSize = Files.size(path.resolve(filename))
                require(totalSize == actualSize) {
                    "$totalSize bytes received, but $actualSize bytes saved in file."
                }
            }
        }
    }

    private fun cleanOriginalOutputStream() {
        try {
            outputStream.flush()
        } catch (ignored: IOException) { }

        try {
            outputStream.close()
        } catch (ignored: IOException) { }
    }

    private fun cleanTempFile() {
        if (!isInMemory) {
            try {
                Files.deleteIfExists(path.resolve(filename))
            } catch (ignored: IOException) { }
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(SmartStreamReceiver::class.java)
        private val random = SecureRandom()
        private const val RETRY_CHECK_TIMES = 3
        private const val ARTIFACT_PREFIX = "artifact_"
        private const val ARTIFACT_SUFFIX = ".temp"

        /**
         * 生成随机文件名
         */
        private fun generateRandomName(): String {
            var randomLong = random.nextLong()
            randomLong = if (randomLong == Long.MIN_VALUE) 0 else abs(randomLong)
            return ARTIFACT_PREFIX + randomLong.toString() + ARTIFACT_SUFFIX
        }
    }
}
