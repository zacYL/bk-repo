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

package com.tencent.bkrepo.common.artifact

import com.tencent.bkrepo.common.artifact.config.ArtifactBeanRegistrar
import com.tencent.bkrepo.common.artifact.event.ArtifactEventListener
import com.tencent.bkrepo.common.artifact.exception.ExceptionConfiguration
import com.tencent.bkrepo.common.artifact.health.ArtifactHealthConfiguration
import com.tencent.bkrepo.common.artifact.metrics.ArtifactMetrics
import com.tencent.bkrepo.common.artifact.permission.ArtifactPermissionCheckHandler
import com.tencent.bkrepo.common.artifact.repository.composite.CompositeRepository
import com.tencent.bkrepo.common.artifact.repository.context.ArtifactContextHolder
import com.tencent.bkrepo.common.artifact.repository.core.StorageManager
import com.tencent.bkrepo.common.artifact.repository.local.LocalRepository
import com.tencent.bkrepo.common.artifact.repository.remote.RemoteRepository
import com.tencent.bkrepo.common.artifact.resolve.ResolverConfiguration
import com.tencent.bkrepo.common.artifact.webhook.WebHookService
import com.tencent.bkrepo.repository.api.ProxyChannelClient
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Import
import org.springframework.context.annotation.PropertySource

@Configuration
@ConditionalOnWebApplication
@PropertySource("classpath:common-artifact.properties")
@Import(
    ArtifactBeanRegistrar::class,
    ResolverConfiguration::class,
    ExceptionConfiguration::class,
    ArtifactMetrics::class,
    ArtifactHealthConfiguration::class,
    ArtifactContextHolder::class,
    ArtifactPermissionCheckHandler::class,
    StorageManager::class
)
class ArtifactAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    fun compositeRepository(
        localRepository: LocalRepository,
        remoteRepository: RemoteRepository,
        proxyChannelClient: ProxyChannelClient
    ): CompositeRepository {
        return CompositeRepository(localRepository, remoteRepository, proxyChannelClient)
    }

    @Bean
    fun artifactEventListener(webHookService: WebHookService) = ArtifactEventListener(webHookService)

    @Bean
    fun webHookService() = WebHookService()
}
