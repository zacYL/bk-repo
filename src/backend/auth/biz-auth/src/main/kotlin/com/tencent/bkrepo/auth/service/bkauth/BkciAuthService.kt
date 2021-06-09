/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
 *
 * Copyright (C) 2021 THL A29 Limited, a Tencent company.  All rights reserved.
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

package com.tencent.bkrepo.auth.service.bkauth

import com.fasterxml.jackson.module.kotlin.readValue
import com.google.common.cache.CacheBuilder
import com.tencent.bkrepo.auth.config.BkAuthConfig
import com.tencent.bkrepo.auth.pojo.BkciAuthCheckResponse
import com.tencent.bkrepo.auth.pojo.BkciAuthListResponse
import com.tencent.bkrepo.auth.pojo.enums.BkAuthPermission
import com.tencent.bkrepo.auth.pojo.enums.BkAuthResourceType
import com.tencent.bkrepo.auth.util.HttpUtils
import com.tencent.bkrepo.common.api.util.JsonUtils.objectMapper
import okhttp3.Request
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.util.concurrent.TimeUnit

@Service
class BkciAuthService @Autowired constructor(
    private val bkAuthConfig: BkAuthConfig
) {
    private val okHttpClient = okhttp3.OkHttpClient.Builder()
        .connectTimeout(3L, TimeUnit.SECONDS)
        .readTimeout(5L, TimeUnit.SECONDS)
        .writeTimeout(5L, TimeUnit.SECONDS)
        .build()

    private val resourcePermissionCache = CacheBuilder.newBuilder()
        .maximumSize(20000)
        .expireAfterWrite(40, TimeUnit.SECONDS)
        .build<String, Boolean>()

    fun isProjectMember(user: String, projectCode: String): Boolean {
        val cacheKey = "$user::$projectCode"
        val cacheResult = resourcePermissionCache.getIfPresent(cacheKey)
        cacheResult?.let {
            logger.debug("match in cache: $cacheKey|$cacheResult")
            return cacheResult
        }

        val url = "${bkAuthConfig.getBkciAuthServer()}/auth/api/open/service/auth/projects/$projectCode" +
            "/users/$user/isProjectUsers"
        logger.debug("validateProjectUsers, requestUrl: [$url]")
        return try {
            val request =
                Request.Builder().url(url).header(DEVOPS_BK_TOKEN, bkAuthConfig.getBkciAuthToken()).get().build()
            val apiResponse = HttpUtils.doRequest(okHttpClient, request, 2)
            val responseObject = objectMapper.readValue<BkciAuthCheckResponse>(apiResponse.content)
            logger.debug("validateProjectUsers  result : [${apiResponse.content}]")
            resourcePermissionCache.put(cacheKey, responseObject.data)
            responseObject.data
        } catch (exception: Exception) {
            logger.warn("validateProjectUsers error: [$exception]")
            false
        }
    }

    fun validateUserResourcePermission(
        user: String,
        projectCode: String,
        action: BkAuthPermission,
        resourceCode: String,
        resourceType: BkAuthResourceType
    ): Boolean {
        val cacheKey = "$user::$projectCode::${resourceType.value}::$resourceCode::${action.value}"
        val cacheResult = resourcePermissionCache.getIfPresent(cacheKey)
        cacheResult?.let {
            logger.debug("match in cache: $cacheKey|$cacheResult")
            return cacheResult
        }

        val url =
            "${bkAuthConfig.getBkciAuthServer()}/auth/api/open/service/auth/permission" +
                "/projects/$projectCode/relation/validate?" +
                "action=${action.value}&resourceCode=$resourceCode&resourceType=${resourceType.value}"
        logger.debug("validateUserResourcePermission, requestUrl: [$url]")
        return try {
            val request = Request.Builder().url(url).header(DEVOPS_BK_TOKEN, bkAuthConfig.getBkciAuthToken())
                .header(DEVOPS_UID, user).get().build()
            val apiResponse = HttpUtils.doRequest(okHttpClient, request, 2)
            val responseObject = objectMapper.readValue<BkciAuthCheckResponse>(apiResponse.content)
            logger.debug("validateUserResourcePermission, result : [${apiResponse.content}]")
            resourcePermissionCache.put(cacheKey, responseObject.data)
            responseObject.data
        } catch (exception: Exception) {
            logger.warn("validateUserResourcePermission error: [$exception]")
            false
        }
    }

    fun getUserResourceByPermission(
        user: String,
        projectCode: String,
        action: BkAuthPermission,
        resourceType: BkAuthResourceType
    ): List<String> {
        return try {
            val url =
                "${bkAuthConfig.getBkciAuthServer()}/auth/api/open/service/auth/permission/" +
                    "projects/$projectCode/action/instance?" +
                    "action=${action.value}&resourceType=${resourceType.value}"
            logger.debug("getUserResourceByPermission, requestUrl: [$url] ")
            val request = Request.Builder().url(url).header(DEVOPS_BK_TOKEN, bkAuthConfig.getBkciAuthToken())
                .header(DEVOPS_UID, user).get().build()
            val apiResponse = HttpUtils.doRequest(okHttpClient, request, 2)
            val responseObject = objectMapper.readValue<BkciAuthListResponse>(apiResponse.content)
            logger.debug("getUserResourceByPermission result : [${apiResponse.content}]")
            return responseObject.data
        } catch (exception: Exception) {
            logger.warn("getUserResourceByPermission error: [$exception]")
            emptyList()
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(BkciAuthService::class.java)
        const val DEVOPS_BK_TOKEN = "X-DEVOPS-BK-TOKEN"
        const val DEVOPS_UID = "X-DEVOPS-UID"
    }
}