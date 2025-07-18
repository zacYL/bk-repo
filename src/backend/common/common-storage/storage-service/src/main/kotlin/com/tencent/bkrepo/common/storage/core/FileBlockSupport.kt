/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
 *
 * Copyright (C) 2021 Tencent.  All rights reserved.
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

package com.tencent.bkrepo.common.storage.core

import com.tencent.bkrepo.common.api.constant.StringPool
import com.tencent.bkrepo.common.artifact.api.ArtifactFile
import com.tencent.bkrepo.common.artifact.api.toArtifactFile
import com.tencent.bkrepo.common.artifact.hash.md5
import com.tencent.bkrepo.common.artifact.hash.sha256
import com.tencent.bkrepo.common.storage.credentials.StorageCredentials
import com.tencent.bkrepo.common.storage.filesystem.FileSystemClient
import com.tencent.bkrepo.common.storage.message.StorageErrorException
import com.tencent.bkrepo.common.storage.message.StorageMessageCode
import com.tencent.bkrepo.common.storage.pojo.FileInfo
import org.slf4j.LoggerFactory
import java.io.File

/**
 * 分块操作实现类
 */
@Suppress("TooGenericExceptionCaught")
abstract class FileBlockSupport : CleanupSupport() {

    override fun createAppendId(storageCredentials: StorageCredentials?): String {
        val appendId = StringPool.uniqueId()
        val credentials = getCredentialsOrDefault(storageCredentials)
        val tempClient = getTempClient(credentials)
        try {
            tempClient.touch(CURRENT_PATH, appendId)
            logger.info("Success to create append id [$appendId]")
            return appendId
        } catch (exception: Exception) {
            logger.error("Failed to create append id [$appendId] on [${credentials.key}]", exception)
            throw StorageErrorException(StorageMessageCode.STORE_ERROR)
        }
    }

    override fun findLengthOfAppendFile(appendId: String, storageCredentials: StorageCredentials?): Long {
        val credentials = getCredentialsOrDefault(storageCredentials)
        val tempClient = getTempClient(credentials)
        try {
            return tempClient.length(CURRENT_PATH, appendId)
        } catch (exception: Exception) {
            logger.error("Failed to read length of id [$appendId] on [${credentials.key}]", exception)
            throw StorageErrorException(StorageMessageCode.STORE_ERROR)
        }
    }

    override fun append(appendId: String, artifactFile: ArtifactFile, storageCredentials: StorageCredentials?): Long {
        val credentials = getCredentialsOrDefault(storageCredentials)
        val tempClient = getTempClient(credentials)
        val inputStream = artifactFile.getInputStream()
        val size = artifactFile.getSize()
        try {
            val length = tempClient.append(CURRENT_PATH, appendId, inputStream, size)
            logger.info("Success to append file [$appendId]")
            return length
        } catch (exception: Exception) {
            logger.error("Failed to append file [$appendId] on [${credentials.key}]", exception)
            throw StorageErrorException(StorageMessageCode.STORE_ERROR)
        }
    }

    override fun finishAppend(
        appendId: String,
        storageCredentials: StorageCredentials?,
        fileInfo: FileInfo?
    ): FileInfo {
        val credentials = getCredentialsOrDefault(storageCredentials)
        val tempClient = getTempClient(credentials)
        try {
            val fileInfo = tempClient.load(CURRENT_PATH, appendId)?.let { storeMergedFile(it, credentials, fileInfo) }
                ?: throw IllegalArgumentException("Append file does not exist.")
            tempClient.delete(CURRENT_PATH, appendId)
            logger.info("Success to finish append file [$appendId], file info [$fileInfo]")
            return fileInfo
        } catch (exception: Exception) {
            logger.error("Failed to finish append file [$appendId] on [${credentials.key}]", exception)
            throw StorageErrorException(StorageMessageCode.STORE_ERROR)
        }
    }

    override fun createBlockId(storageCredentials: StorageCredentials?): String {
        val blockId = StringPool.uniqueId()
        val credentials = getCredentialsOrDefault(storageCredentials)
        val tempClient = getTempClient(credentials)
        try {
            tempClient.createDirectory(CURRENT_PATH, blockId)
            logger.info("Success to create block [$blockId]")
            return blockId
        } catch (exception: Exception) {
            logger.error("Failed to create block [$blockId] on [${credentials.key}]", exception)
            throw StorageErrorException(StorageMessageCode.STORE_ERROR)
        }
    }

    override fun checkBlockId(blockId: String, storageCredentials: StorageCredentials?): Boolean {
        val credentials = getCredentialsOrDefault(storageCredentials)
        val tempClient = getTempClient(credentials)
        try {
            return tempClient.checkDirectory(blockId)
        } catch (exception: Exception) {
            logger.error("Failed to check block [$blockId] on [${credentials.key}]", exception)
            throw StorageErrorException(StorageMessageCode.STORE_ERROR)
        }
    }

    override fun storeBlock(
        blockId: String,
        sequence: Int,
        digest: String,
        artifactFile: ArtifactFile,
        overwrite: Boolean,
        storageCredentials: StorageCredentials?,
    ) {
        val credentials = getCredentialsOrDefault(storageCredentials)
        val tempClient = getTempClient(credentials)
        val blockInputStream = artifactFile.getInputStream()
        val blockSize = artifactFile.getSize()
        val digestInputStream = digest.byteInputStream()
        val digestSize = digest.length.toLong()
        try {
            tempClient.store(blockId, "$sequence$BLOCK_SUFFIX", blockInputStream, blockSize, overwrite)
            tempClient.store(blockId, "$sequence$SHA256_SUFFIX", digestInputStream, digestSize, overwrite)
            logger.info("Success to store block [$blockId/$sequence]")
        } catch (exception: Exception) {
            logger.error("Failed to store block [$blockId/$sequence] on [${credentials.key}]", exception)
            throw StorageErrorException(StorageMessageCode.STORE_ERROR)
        }
    }

    override fun storeBlockWithRandomPosition(
        blockId: String,
        sequence: Int,
        digest: String,
        artifactFile: ArtifactFile,
        overwrite: Boolean,
        storageCredentials: StorageCredentials?,
        startPosition: Long,
        totalLength: Long,
    ) {
        val credentials = getCredentialsOrDefault(storageCredentials)
        val tempClient = getTempClient(credentials)
        val blockInputStream = artifactFile.getInputStream()
        val blockFileSize = artifactFile.getSize()
        try {
            tempClient.store(
                blockId, "$sequence$SHA256_SUFFIX",
                digest.byteInputStream(), digest.length.toLong(), overwrite
            )
            tempClient.store(
                blockId, "$sequence$BLOCK_APPEND_SUFFIX",
                digest.byteInputStream(), digest.length.toLong(), overwrite
            )
            tempClient.appendAt(
                blockId, MERGED_FILENAME, blockInputStream,
                blockFileSize, startPosition, totalLength
            )
            logger.info("Success to append block [$blockId/$sequence] at position $startPosition")
        } catch (exception: Exception) {
            logger.error("Failed to store block [$blockId/$sequence] on [${credentials.key}]", exception)
            tempClient.delete(blockId, "$sequence$BLOCK_APPEND_SUFFIX")
            tempClient.delete(blockId, "$sequence$SHA256_SUFFIX")
            throw StorageErrorException(StorageMessageCode.STORE_ERROR)
        }
    }

    override fun mergeBlock(
        blockId: String,
        storageCredentials: StorageCredentials?,
        fileInfo: FileInfo?,
        mergeFileFlag: Boolean
    ): FileInfo {
        val credentials = getCredentialsOrDefault(storageCredentials)
        val tempClient = getTempClient(credentials)
        try {
            var mergedFile = tempClient.touch(
                blockId,
                MERGED_FILENAME
            )
            if (mergeFileFlag) {
                val blockFileList = getBlockList(tempClient, blockId, BLOCK_SUFFIX)
                mergedFile = tempClient.mergeFiles(
                    blockFileList, mergedFile, mergeFileFlag
                )
            } else {
                getBlockList(tempClient, blockId, BLOCK_APPEND_SUFFIX)
            }
            val fileInfo = storeMergedFile(mergedFile, credentials, fileInfo)
            tempClient.deleteDirectory(CURRENT_PATH, blockId)
            logger.info("Success to merge block [$blockId]")
            return fileInfo
        } catch (storageException: StorageErrorException) {
            logger.error("Failed to merge block [$blockId] on [${credentials.key}]: ${storageException.messageCode}")
            throw storageException
        } catch (exception: Exception) {
            logger.error("Failed to merge block [$blockId] on [${credentials.key}]", exception)
            throw StorageErrorException(StorageMessageCode.STORE_ERROR)
        }
    }

    override fun deleteBlockId(blockId: String, storageCredentials: StorageCredentials?) {
        val credentials = getCredentialsOrDefault(storageCredentials)
        val tempClient = getTempClient(credentials)
        try {
            tempClient.deleteDirectory(CURRENT_PATH, blockId)
            logger.info("Success to delete block id [$blockId]")
        } catch (exception: Exception) {
            logger.error("Failed to delete block id [$blockId] on [${credentials.key}]", exception)
            throw StorageErrorException(StorageMessageCode.STORE_ERROR)
        }
    }

    override fun listBlock(blockId: String, storageCredentials: StorageCredentials?): List<Triple<Long, String, Int>> {
        val credentials = getCredentialsOrDefault(storageCredentials)
        val tempClient = getTempClient(credentials)
        try {
            val blockFileList = tempClient.listFiles(blockId, BLOCK_SUFFIX).sortedBy {
                it.name.removeSuffix(BLOCK_SUFFIX).toInt()
            }
            return blockFileList.map {
                val size = it.length()
                val name = it.name.replace(BLOCK_SUFFIX, SHA256_SUFFIX)
                val sha256 = tempClient.load(blockId, name)?.readText().orEmpty()
                val sequence = it.name.removeSuffix(BLOCK_SUFFIX).toInt()
                Triple(size, sha256, sequence)
            }
        } catch (exception: Exception) {
            logger.error("Failed to list block [$blockId] on [${credentials.key}]", exception)
            throw StorageErrorException(StorageMessageCode.STORE_ERROR)
        }
    }

    /**
     * 合并文件并返回对应FileInfo(sha256、md5、size)
     * 当 fileInfo不为空时：避免当文件过大时生成 sha256 或者 md5 需要过长时间，信任传递进来的 sha256 和md5 值
     * 当 fileInfo为空时，生成对应的 sha256 或者 md5
     */
    private fun storeMergedFile(file: File, credentials: StorageCredentials, fileInfo: FileInfo? = null): FileInfo {
        val size = file.length()
        val realFileInfo = if (fileInfo == null) {
            FileInfo(file.sha256(), file.md5(), size)
        } else {
            if (fileInfo.size != size)
                throw IllegalArgumentException("Merged file is broken!")
            FileInfo(fileInfo.sha256, fileInfo.md5, size)
        }
        val path = fileLocator.locate(realFileInfo.sha256)
        if (!doExist(path, realFileInfo.sha256, credentials)) {
            doStore(path, realFileInfo.sha256, file.toArtifactFile(), credentials)
        } else {
            logger.info("File [${realFileInfo.sha256}] exist, skip store.")
        }
        return realFileInfo
    }


    private fun getBlockList(tempClient: FileSystemClient, blockId: String, suffixString: String): List<File> {
        val blockFileList = tempClient.listFiles(blockId, suffixString).sortedBy {
            it.name.removeSuffix(suffixString).toInt()
        }
        blockFileList.takeIf { it.isNotEmpty() } ?: throw StorageErrorException(StorageMessageCode.BLOCK_EMPTY)
        for (index in blockFileList.indices) {
            val sequence = index + 1
            if (blockFileList[index].name.removeSuffix(suffixString).toInt() != sequence) {
                throw StorageErrorException(StorageMessageCode.BLOCK_MISSING, sequence.toString())
            }
        }
        return blockFileList
    }

    companion object {
        private val logger = LoggerFactory.getLogger(FileBlockSupport::class.java)
        private const val CURRENT_PATH = StringPool.EMPTY
        private const val BLOCK_SUFFIX = ".block"
        private const val SHA256_SUFFIX = ".sha256"
        private const val BLOCK_APPEND_SUFFIX = ".blockAppend"
        private const val MERGED_FILENAME = "merged.data"
    }
}
