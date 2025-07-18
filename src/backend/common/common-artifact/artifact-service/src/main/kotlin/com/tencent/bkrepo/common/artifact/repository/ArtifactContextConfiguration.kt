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

package com.tencent.bkrepo.common.artifact.repository

import com.tencent.bkrepo.common.artifact.config.ArtifactBeanRegistrar
import com.tencent.bkrepo.common.artifact.repository.composite.CompositeRepository
import com.tencent.bkrepo.common.artifact.repository.context.ArtifactClient
import com.tencent.bkrepo.common.artifact.repository.context.ArtifactContextHolder
import com.tencent.bkrepo.common.artifact.repository.proxy.ProxyRepository
import com.tencent.bkrepo.common.artifact.repository.redirect.CosRedirectService
import com.tencent.bkrepo.common.artifact.repository.redirect.DownloadRedirectManager
import com.tencent.bkrepo.common.artifact.repository.redirect.EdgeNodeRedirectService
import com.tencent.bkrepo.common.artifact.repository.redirect.LinkNodeRedirectService
import com.tencent.bkrepo.common.artifact.repository.remote.AsyncRemoteArtifactCacheWriter
import com.tencent.bkrepo.common.artifact.repository.remote.DefaultAsyncCacheHttpClientBuilderFactory
import com.tencent.bkrepo.common.artifact.repository.remote.RemoteArtifactCacheLocks
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Import

@Configuration
@Import(
    ArtifactBeanRegistrar::class,
    ArtifactContextHolder::class,
    CompositeRepository::class,
    ProxyRepository::class,
    EdgeNodeRedirectService::class,
    CosRedirectService::class,
    LinkNodeRedirectService::class,
    DownloadRedirectManager::class,
    AsyncRemoteArtifactCacheWriter::class,
    DefaultAsyncCacheHttpClientBuilderFactory::class,
    RemoteArtifactCacheLocks::class
)
class ArtifactContextConfiguration {

    @Bean
    @ConditionalOnMissingBean
    fun artifactClient(): ArtifactClient {
        return ArtifactClient()
    }
}
