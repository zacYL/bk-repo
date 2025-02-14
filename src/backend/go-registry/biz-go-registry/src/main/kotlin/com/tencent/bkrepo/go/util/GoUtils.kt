/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
 *
 * Copyright (C) 2024 THL A29 Limited, a Tencent company.  All rights reserved.
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

package com.tencent.bkrepo.go.util

import com.tencent.bkrepo.common.metadata.util.version.SemVersion
import com.tencent.bkrepo.go.constant.CLIENT_ARCHIVE_PATH
import com.tencent.bkrepo.go.constant.CLIENT_NAME
import com.tencent.bkrepo.go.constant.VERSION_PREFIX
import com.tencent.bkrepo.go.util.DecompressUtil.listFile
import java.util.*

object GoUtils {

    private val clientList by lazy { listAllClientFile() }

    fun String.caseDecode() = this.replace(Regex("!([a-z])")) {
        it.groupValues[1].uppercase(Locale.getDefault())
    }

    fun String.caseEncode() = this.replace(Regex("[A-Z]")) { "%21${it.value.lowercase(Locale.getDefault())}" }

    fun String.convertToSemver() = SemVersion.parse(this.removePrefix(VERSION_PREFIX))

    fun getNewerClientVersion(userAgent: String?): String? {
        return getClientInfo(userAgent)?.run {
            val latest = getLatestClientVersion(second, third)
            if (latest != null && SemVersion.parse(latest) > SemVersion.parse(first)) latest else null
        }
    }

    fun getLatestClientVersion(os: String, arch: String) = listClientFile(os, arch)
        .maxByOrNull { SemVersion.parse(getClientVersionFromName(it)) }?.let { getClientVersionFromName(it) }

    fun listClientFile(os: String? = null, arch: String? = null): List<String> {
        val prefix = if (os.isNullOrBlank() || arch.isNullOrBlank()) "$CLIENT_NAME-" else "$CLIENT_NAME-$os-$arch-"
        return clientList.filter { it.startsWith(prefix) }
    }

    private fun listAllClientFile(): List<String> {
        return this.javaClass.classLoader.getResourceAsStream(CLIENT_ARCHIVE_PATH)?.use { it.listFile() } ?: emptyList()
    }

    private fun getClientInfo(userAgent: String?): Triple<String, String, String>? {
        if (userAgent.isNullOrBlank() || !userAgent.startsWith(CLIENT_NAME)) return null
        val map = mutableMapOf<String, String>()
        userAgent.split(" ").forEach {
            val pair = it.split("/")
            map[pair.first()] = pair.getOrNull(1).orEmpty()
        }
        val version = map[CLIENT_NAME]
        val os = map["os"]
        val arch = map["arch"]
        return if (version.isNullOrBlank() || os.isNullOrBlank() || arch.isNullOrBlank()) null
            else Triple(version, os, arch)
    }

    private fun getClientVersionFromName(name: String) = name.substringAfterLast("-").trimStart('v')
}
