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

package com.tencent.bkrepo.conan.artifact.repository

import com.tencent.bkrepo.common.artifact.exception.ArtifactNotFoundException
import com.tencent.bkrepo.common.artifact.pojo.RepositoryCategory
import com.tencent.bkrepo.common.artifact.repository.context.ArtifactContextHolder
import com.tencent.bkrepo.common.artifact.repository.context.ArtifactDownloadContext
import com.tencent.bkrepo.common.artifact.repository.context.ArtifactQueryContext
import com.tencent.bkrepo.common.artifact.repository.core.AbstractArtifactRepository
import com.tencent.bkrepo.common.artifact.repository.virtual.VirtualRepository
import com.tencent.bkrepo.common.artifact.resolve.response.ArtifactResource
import com.tencent.bkrepo.common.redis.RedisOperation
import com.tencent.bkrepo.common.service.util.HttpContextHolder
import com.tencent.bkrepo.conan.constant.IGNORECASE
import com.tencent.bkrepo.conan.constant.PATTERN
import com.tencent.bkrepo.conan.constant.REQUEST_TYPE
import com.tencent.bkrepo.conan.pojo.ConanSearchResult
import com.tencent.bkrepo.conan.pojo.artifact.ConanArtifactInfo
import com.tencent.bkrepo.conan.pojo.enums.ConanRequestType
import com.tencent.bkrepo.conan.utils.PathUtils.extractConanFileReference
import com.tencent.bkrepo.conan.utils.PathUtils.getConanRecipePattern
import com.tencent.bkrepo.repository.pojo.repo.RepositoryDetail
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import org.springframework.web.context.request.RequestContextHolder

@Component
class ConanVirtualRepository : VirtualRepository() {

    @Autowired
    lateinit var redisOperation: RedisOperation

    @Autowired
    lateinit var conanRemoteRepository: ConanRemoteRepository

    @Autowired
    lateinit var conanLocalRepository: ConanLocalRepository

    override fun query(context: ArtifactQueryContext): Any {
        return searchResult(context)
    }

    private fun searchResult(context: ArtifactQueryContext): ConanSearchResult {
        val virtualConfiguration = context.getVirtualConfiguration()
        val repoList = virtualConfiguration.repositoryList
        // 分隔出本地仓库和远程仓库
        val repoCategoryMap = repoList.map {
            repositoryClient.getRepoDetail(context.projectId, it.name).data!!
        }.groupBy { it.category }

        val recipeRepoMap = mutableMapOf<String, String>()
        val aggregateResult = mutableSetOf<String>()

        fun getRecipes(repos: List<RepositoryDetail>, queryFunction: (ArtifactQueryContext) -> ConanSearchResult) {
            repos.map { repoDetail ->
                ArtifactQueryContext(repoDetail).let { queryContext ->
                    context.getAttribute<String>(PATTERN)?.let { queryContext.putAttribute(PATTERN, it) }
                    context.getAttribute<Boolean>(IGNORECASE)?.let { queryContext.putAttribute(IGNORECASE, it) }
                    queryContext.putAttribute(REQUEST_TYPE, ConanRequestType.SEARCH)
                    try {
                        queryFunction(queryContext).results.forEach { recipe ->
                            recipeRepoMap.putIfAbsent(recipe, repoDetail.name)
                            aggregateResult.add(recipe)
                        }
                    } catch (e: Exception) {
                        logger.warn("virtual repository query error, repo: ${repoDetail.name}, error: $e")
                    }
                }
            }
        }

        repoCategoryMap[RepositoryCategory.REMOTE]?.let { remoteRepos ->
            getRecipes(remoteRepos) { conanRemoteRepository.query(it) as ConanSearchResult }
        }

        repoCategoryMap[RepositoryCategory.LOCAL]?.let { localRepos ->
            getRecipes(localRepos) { conanLocalRepository.query(it) as ConanSearchResult }
        }
        cacheRecipes(context.projectId, context.repoName, recipeRepoMap)
        return ConanSearchResult(aggregateResult.sorted())
    }

    /**
     * 获取缓存的recipe对应的仓库名。如果是远程仓库，则在ProxyInterceptor已经转到远程仓库，此处处理的是本地仓库
     * @see com.tencent.bkrepo.conan.servelet.ProxyInterceptor
     */
    override fun onDownload(context: ArtifactDownloadContext): ArtifactResource? {
        with(context) {
            getFullPathInterceptors().forEach { it.intercept(projectId, artifactInfo.getArtifactFullPath()) }
            val cacheKey = getRecipeCacheKey(projectId, repoName, getConanRecipePattern(HttpContextHolder.getRequest().requestURI.extractConanFileReference()))
            return redisOperation.get(cacheKey)?.let { it ->
                logger.info("recipe $cacheKey is in cache, redirect to repo $it")
                val subRepoDetail = repositoryClient.getRepoDetail(repo.projectId, "conan0813_local").data!!
                val repository = ArtifactContextHolder.getRepository(subRepoDetail.category)
                val subContext = generateSubContext(context, subRepoDetail)
                require(subContext is ArtifactDownloadContext)
                require(repository is ConanLocalRepository)
                repository.onDownloadBefore(subContext)
                val resource = repository.onDownload(subContext)
                return resource
            } ?: super.onDownload(context)
        }
    }

    private fun cacheRecipes(projectId: String, virtualRepoName: String, recipeRepoMap: MutableMap<String, String>) {
        recipeRepoMap.forEach { (recipeName, repoName) ->
            logger.info("cache recipe $recipeName to repo $repoName")
            redisOperation.set(getRecipeCacheKey(projectId, virtualRepoName, recipeName), repoName, 60 * 5)
        }
    }

    companion object {

        private val logger = LoggerFactory.getLogger(ConanVirtualRepository::class.java)

        private const val CONAN_VIR_PREFIX = "conan:virtual:"

        /**
         * key为虚拟仓库的包名，value为对应的仓库名
         */
        fun getRecipeCacheKey(projectId: String, repoName: String, recipeName: String): String {
            return "$CONAN_VIR_PREFIX$projectId/$repoName/$recipeName"
        }
    }

}

