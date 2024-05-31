/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
 *
 * Copyright (C) 2022 THL A29 Limited, a Tencent company.  All rights reserved.
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

package com.tencent.bkrepo.conan.service.impl

import com.tencent.bkrepo.common.redis.RedisOperation
import com.tencent.bkrepo.common.service.util.SpringContextUtils
import com.tencent.bkrepo.conan.artifact.repository.ConanVirtualRepository
import com.tencent.bkrepo.conan.exception.ConanException
import com.tencent.bkrepo.conan.pojo.ConanFileReference
import com.tencent.bkrepo.conan.pojo.artifact.ConanArtifactInfo
import com.tencent.bkrepo.conan.service.ConanSearchService
import com.tencent.bkrepo.conan.service.ConanVirtualService
import com.tencent.bkrepo.conan.utils.PathUtils.extractConanFileReference
import com.tencent.bkrepo.conan.utils.PathUtils.getConanRecipePattern
import com.tencent.bkrepo.repository.pojo.repo.RepositoryDetail
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class ConanVirtualServiceImpl : ConanVirtualService {

    @Autowired
    lateinit var redisOperation: RedisOperation

    override fun getOrSetCacheRepo(repositoryDetail: RepositoryDetail, requestURI: String, artifactInfo: ConanArtifactInfo): String {
        with(repositoryDetail) {
            val conanFileReference = requestURI.extractConanFileReference()
            val cacheKey = ConanVirtualRepository.getRecipeCacheKey(projectId, name, getConanRecipePattern(conanFileReference))

            var cacheRepo: String? = null
            var retryCount = 0
            while (retryCount < 3) {
                redisOperation.keys(cacheKey).firstOrNull()?.let {
                    cacheRepo = redisOperation.get(it)
                }
                if (cacheRepo != null) {
                    return cacheRepo as String
                }
                selectRepo(conanFileReference, artifactInfo)
                retryCount++
            }

            // 如果重试3次后仍然无法获取缓存，则抛出异常
            throw ConanException("Cannot find cache repo after 3 retries")
        }
    }

    override fun getCacheRepo(repositoryDetail: RepositoryDetail, requestURI: String): String? {
        with(repositoryDetail) {
            val conanFileReference = requestURI.extractConanFileReference()
            val cacheKey = ConanVirtualRepository.getRecipeCacheKey(projectId, name, getConanRecipePattern(conanFileReference))
            return redisOperation.keys(cacheKey).firstOrNull()?.let {
                redisOperation.get(it)
            }
        }
    }

    private fun selectRepo(conanFileReference: ConanFileReference, artifactInfo: ConanArtifactInfo): String {
        return SpringContextUtils.getBean(ConanSearchService::class.java).search(artifactInfo, conanFileReference.name, true).results.firstOrNull()
            ?: throw ConanException("can not find cache repo")
    }

}
