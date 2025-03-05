/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
 *
 * Copyright (C) 2019 THL A29 Limited, a Tencent company.  All rights reserved.
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

package com.tencent.bkrepo.common.metadata.interceptor

import com.tencent.bkrepo.common.api.constant.DOWNLOAD_SOURCE
import com.tencent.bkrepo.common.api.constant.HttpHeaders
import com.tencent.bkrepo.common.artifact.constant.DownloadInterceptorType
import com.tencent.bkrepo.common.artifact.constant.FORBID_STATUS
import com.tencent.bkrepo.common.artifact.constant.LOCK_STATUS
import com.tencent.bkrepo.common.metadata.interceptor.config.DownloadInterceptorProperties
import com.tencent.bkrepo.common.metadata.interceptor.impl.FilenameInterceptor
import com.tencent.bkrepo.common.metadata.interceptor.impl.IpSegmentInterceptor
import com.tencent.bkrepo.common.metadata.interceptor.impl.MetadataInterceptor
import com.tencent.bkrepo.common.metadata.interceptor.impl.MobileInterceptor
import com.tencent.bkrepo.common.metadata.interceptor.impl.NodeMetadataInterceptor
import com.tencent.bkrepo.common.metadata.interceptor.impl.OfficeNetworkInterceptor
import com.tencent.bkrepo.common.metadata.interceptor.impl.PackageMetadataInterceptor
import com.tencent.bkrepo.common.metadata.interceptor.impl.PathPatternInterceptor
import com.tencent.bkrepo.common.metadata.interceptor.impl.WebInterceptor
import com.tencent.bkrepo.common.service.util.HeaderUtils
import com.tencent.bkrepo.common.service.util.HttpContextHolder
import com.tencent.bkrepo.common.service.util.SpringContextUtils
import com.tencent.bkrepo.repository.pojo.node.NodeDetail
import com.tencent.bkrepo.repository.pojo.packages.PackageVersion
import org.slf4j.LoggerFactory

class DownloadInterceptorFactory(
    properties: DownloadInterceptorProperties
) {

    init {
        Companion.properties = properties
    }

    companion object {
        val logger = LoggerFactory.getLogger(DownloadInterceptorFactory::class.java)
        private lateinit var properties: DownloadInterceptorProperties
        const val ANDROID_APP_USER_AGENT = "BKCI_APP"
        const val ANDROID_APP_USER_AGENT_NEW = "BK_CI APP"
        const val IOS_APP_USER_AGENT = "com.apple.appstored"
        const val INTERCEPTORS = "interceptors"
        const val TYPE = "type"
        const val RULES = "rules"
        private val forbidRule = mapOf(
            MetadataInterceptor.METADATA to "$FORBID_STATUS:true",
            DownloadInterceptor.ALLOWED to false
        )

        private val lockRule = mapOf(
            MetadataInterceptor.METADATA to "$LOCK_STATUS:true",
            DownloadInterceptor.ALLOWED to false
        )

        @Suppress("UNCHECKED_CAST")
        inline fun <reified A> buildInterceptors(
            settings: MutableMap<String, Any>
        ): List<DownloadInterceptor<*, A>> {
            val interceptorList = mutableListOf<DownloadInterceptor<*, A>>()
            try {
                val interceptors = settings[INTERCEPTORS] as? List<Map<String, Any>>
                interceptors?.forEach {
                    val type: DownloadInterceptorType = DownloadInterceptorType.valueOf(it[TYPE].toString())
                    val rules = it[RULES] as Map<String, Any>
                    val interceptor = when (A::class) {
                        NodeDetail::class -> buildNodeInterceptor(type, rules) as DownloadInterceptor<*, A>?
                        String::class -> buildFullPathInterceptor(type, rules) as DownloadInterceptor<*, A>?
                        else -> null
                    }
                    interceptor?.let { interceptorList.add(interceptor) }
                }
                if (A::class == NodeDetail::class) {
                    interceptorList.add(
                        buildNodeInterceptor(DownloadInterceptorType.NODE_FORBID)!! as DownloadInterceptor<*, A>
                    )
                }
                val customInterceptors = SpringContextUtils.getBeansWithType(DownloadInterceptor::class.java)
                    .map { it as DownloadInterceptor<*, NodeDetail> }
                interceptorList.addAll(customInterceptors)
            } catch (e: Exception) {
                logger.warn("fail to get download interceptor by settings[$settings]: $e")
            }
            return interceptorList
        }

        fun buildNodeInterceptor(
            type: DownloadInterceptorType,
            rules: Map<String, Any> = emptyMap()
        ): DownloadInterceptor<*, *>? {
            val downloadSource = getDownloadSource()
            return when {
                type == DownloadInterceptorType.FILENAME -> FilenameInterceptor(rules)
                type == DownloadInterceptorType.METADATA -> NodeMetadataInterceptor(rules)
                type == DownloadInterceptorType.WEB && type == downloadSource -> WebInterceptor(rules)
                type == DownloadInterceptorType.MOBILE && type == downloadSource -> MobileInterceptor(rules)
                type == DownloadInterceptorType.OFFICE_NETWORK ->
                    OfficeNetworkInterceptor<NodeDetail>(rules, properties)
                type == DownloadInterceptorType.NODE_FORBID -> buildNodeForbidInterceptor()
                type == DownloadInterceptorType.IP_SEGMENT -> IpSegmentInterceptor(rules, properties)
                type == DownloadInterceptorType.NODE_LOCK -> NodeMetadataInterceptor(lockRule)
                else -> null
            }
        }

        fun buildNodeForbidInterceptor(): DownloadInterceptor<*, NodeDetail> {
            return NodeMetadataInterceptor(forbidRule)
        }

        fun buildPackageInterceptor(type: DownloadInterceptorType): DownloadInterceptor<*, PackageVersion>? {
            return when(type) {
                DownloadInterceptorType.PACKAGE_FORBID -> PackageMetadataInterceptor(forbidRule)
                DownloadInterceptorType.PACKAGE_LOCK -> PackageMetadataInterceptor(lockRule)
                else -> null
            }
        }

        fun buildFullPathInterceptor(
            type: DownloadInterceptorType,
            settings: Map<String, Any>
        ): DownloadInterceptor<*, String>? {
            return when (type) {
                DownloadInterceptorType.PATH_PATTERN -> PathPatternInterceptor(settings)
                else -> null
            }
        }

        private fun getDownloadSource(): DownloadInterceptorType {
            val downloadSource = HttpContextHolder.getRequestOrNull()?.getAttribute(DOWNLOAD_SOURCE)?.toString()
            if (!downloadSource.isNullOrBlank()) {
                return DownloadInterceptorType.valueOf(downloadSource)
            }
            val userAgent = HeaderUtils.getHeader(HttpHeaders.USER_AGENT) ?: return DownloadInterceptorType.WEB
            logger.debug("download user agent: $userAgent")
            val android = userAgent.contains(ANDROID_APP_USER_AGENT) || userAgent.contains(ANDROID_APP_USER_AGENT_NEW)
            return when {
                android -> DownloadInterceptorType.MOBILE
                userAgent.contains(IOS_APP_USER_AGENT) -> DownloadInterceptorType.MOBILE
                else -> DownloadInterceptorType.WEB
            }
        }
    }
}
