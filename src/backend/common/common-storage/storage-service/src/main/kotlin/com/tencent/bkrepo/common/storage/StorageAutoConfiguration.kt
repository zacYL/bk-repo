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

package com.tencent.bkrepo.common.storage

import com.tencent.bkrepo.common.api.exception.SystemErrorException
import com.tencent.bkrepo.common.api.message.CommonMessageCode
import com.tencent.bkrepo.common.storage.core.FileStorage
import com.tencent.bkrepo.common.storage.config.StorageProperties
import com.tencent.bkrepo.common.storage.core.StorageService
import com.tencent.bkrepo.common.storage.core.cache.CacheStorageService
import com.tencent.bkrepo.common.storage.core.cache.indexer.StorageCacheIndexConfiguration
import com.tencent.bkrepo.common.storage.core.locator.FileLocator
import com.tencent.bkrepo.common.storage.core.locator.HashFileLocator
import com.tencent.bkrepo.common.storage.core.simple.SimpleStorageService
import com.tencent.bkrepo.common.storage.credentials.StorageType
import com.tencent.bkrepo.common.storage.filesystem.FileSystemStorage
import com.tencent.bkrepo.common.storage.filesystem.cleanup.FileRetainResolver
import com.tencent.bkrepo.common.storage.innercos.InnerCosFileStorage
import com.tencent.bkrepo.common.storage.innercos.metrics.CosUploadMetrics
import com.tencent.bkrepo.common.storage.monitor.StorageHealthMonitor
import com.tencent.bkrepo.common.storage.monitor.StorageHealthMonitorHelper
import com.tencent.bkrepo.common.storage.s3.S3Storage
import com.tencent.bkrepo.common.storage.util.PolarisUtil
import com.tencent.bkrepo.common.storage.util.StorageUtils
import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Import
import org.springframework.retry.annotation.EnableRetry
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor
import java.util.concurrent.ConcurrentHashMap

/**
 * 存储自动配置
 */
@Configuration(proxyBeanMethods = false)
@EnableRetry
@EnableConfigurationProperties(StorageProperties::class)
@Import(
    StorageUtils::class,
    StorageCacheIndexConfiguration::class,
    CosUploadMetrics::class,
)
class StorageAutoConfiguration {

    @Bean
    fun fileStorage(
        properties: StorageProperties,
        executor: ThreadPoolTaskExecutor,
    ): FileStorage {
        val fileStorage = when (properties.type) {
            StorageType.FILESYSTEM -> FileSystemStorage()
            StorageType.INNERCOS -> InnerCosFileStorage()
            StorageType.HDFS -> throw SystemErrorException(CommonMessageCode.SYSTEM_ERROR)
            StorageType.S3 -> S3Storage(executor)
            else -> FileSystemStorage()
        }
        logger.info("Initializing FileStorage[${fileStorage::class.simpleName}]")
        return fileStorage
    }

    @Bean
    @ConditionalOnMissingBean
    fun storageService(
        properties: StorageProperties,
        threadPoolTaskExecutor: ThreadPoolTaskExecutor,
        fileRetainResolver: FileRetainResolver?,
    ): StorageService {
        fileRetainResolver?.let { logger.info("Use FileRetainResolver[${fileRetainResolver::class.simpleName}].") }
        val cacheEnabled = properties.defaultStorageCredentials().cache.enabled
        val storageService = if (cacheEnabled) {
            CacheStorageService(threadPoolTaskExecutor, fileRetainResolver)
        } else {
            SimpleStorageService()
        }
        logger.info("Initializing StorageService[${storageService::class.simpleName}].")
        return storageService
    }

    @Bean
    fun storageHealthMonitorHelper(storageProperties: StorageProperties): StorageHealthMonitorHelper {
        return StorageHealthMonitorHelper(ConcurrentHashMap<String, StorageHealthMonitor>())
    }

    @Bean
    @ConditionalOnMissingBean(FileLocator::class)
    fun fileLocator() = HashFileLocator()

    @Bean
    fun polarisUtil(storageProperties: StorageProperties) = PolarisUtil(storageProperties)

    companion object {
        private val logger = LoggerFactory.getLogger(StorageAutoConfiguration::class.java)
    }
}
