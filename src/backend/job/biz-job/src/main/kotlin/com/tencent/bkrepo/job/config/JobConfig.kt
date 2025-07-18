/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
 *
 * Copyright (C) 2019 Tencent.  All rights reserved.
 *
 * BK-CI 蓝鲸持续集成平台 is licensed under the MIT license.
 *
 * A copy of the MIT License is included in this file.
 *
 *
 * Terms of the MIT License:
 * ---------------------------------------------------
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
 * documentation files (the "Software"), to deal in the Software without restriction, including without limitation the
 * rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to
 * permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of
 * the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT
 * LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN
 * NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
 * SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package com.tencent.bkrepo.job.config

import com.tencent.bkrepo.common.artifact.event.base.ArtifactEvent
import com.tencent.bkrepo.job.backup.config.DataBackupConfig
import com.tencent.bkrepo.job.executor.BlockThreadPoolTaskExecutorDecorator
import com.tencent.bkrepo.job.migrate.config.MigrateRepoStorageProperties
import com.tencent.bkrepo.job.separation.config.DataSeparationConfig
import com.tencent.bkrepo.job.separation.listener.SeparationRecoveryEventConsumer
import org.springframework.boot.autoconfigure.task.TaskExecutionProperties
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.messaging.Message
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor
import java.util.function.Consumer

/**
 * Job配置
 * */
@Configuration
@EnableConfigurationProperties(
    JobProperties::class,
    MigrateRepoStorageProperties::class,
    DataSeparationConfig::class,
    DataBackupConfig::class
)
class JobConfig {
    @Bean
    fun blockThreadPoolTaskExecutorDecorator(
        threadPoolTaskExecutor: ThreadPoolTaskExecutor,
        properties: TaskExecutionProperties
    ): BlockThreadPoolTaskExecutorDecorator {
        return BlockThreadPoolTaskExecutorDecorator(
            threadPoolTaskExecutor,
            properties.pool.queueCapacity,
            Runtime.getRuntime().availableProcessors()
        )
    }

    @Bean("separationRecovery")
    fun separationRecoveryEventConsumer(
        separationRecoveryEventConsumer: SeparationRecoveryEventConsumer
    ): Consumer<Message<ArtifactEvent>> {
        return Consumer {
            separationRecoveryEventConsumer.accept(it)
        }
    }
}
