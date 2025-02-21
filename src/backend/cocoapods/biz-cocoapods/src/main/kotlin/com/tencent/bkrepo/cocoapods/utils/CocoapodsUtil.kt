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

package com.tencent.bkrepo.cocoapods.utils

import ArchiveModifier
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.tencent.bkrepo.cocoapods.model.TCocoapodsRemotePackage.Source
import com.tencent.bkrepo.cocoapods.pojo.enums.PodSpecSourceType
import com.tencent.bkrepo.cocoapods.pojo.enums.PodSpecType
import org.apache.commons.lang3.StringUtils

object CocoapodsUtil {

    /**
     * 处理 podspec 文件，将 s.source 替换为 s.source = { :http => "xxx", :type => 'tgz' }
     */
    fun updatePodspecSource(podspecContent: String, tarFilePath: String): String {
        val lines = podspecContent.lines().toMutableList() // 将内容按行分割成列表
        val targetPrefix = "s.source" // 目标行的前缀
        val newSource = { level: String -> """${level}s.source = { :http => "$tarFilePath", :type => 'tgz' }""" }
        var insideSourceBlock = false // 用于标识是否在 `s.source` 定义中
        var startIndex: Int? = null // 记录 `s.source` 的起始索引

        // 遍历找到目标行并替换
        for (i in lines.indices) {
            val trimmedLine = lines[i].trimStart()
            if (trimmedLine.startsWith(targetPrefix)) { // 定位到 `s.source` 开头的行
                startIndex = i
                insideSourceBlock = true
            }
            if (insideSourceBlock) {
                // 检查是否到达定义结束，以 `}` 结束
                if (trimmedLine.endsWith("}")) {
                    // 替换 `s.source` 的内容,获取起始行的缩进
                    val indent =
                        lines[startIndex!!].substring(0, lines[startIndex].indexOf(lines[startIndex].trimStart()))
                    lines[startIndex] = newSource(indent) // 使用新的 source 内容替换起始行
                    for (j in startIndex + 1..i) {
                        lines[j] = "" // 清空多行定义的剩余部分
                    }
                    break
                }
            }
        }

        return lines.joinToString("\n")
    }

    /**
     * 处理podspec.json文件，将s.source替换为s.source = { :http => "xxx", :type => 'tgz' }
     */
    fun updatePodspecJsonSource(podspecJsonContent: String, tarFilePath: String): String {
        val jsonObject = JsonParser.parseString(podspecJsonContent).asJsonObject
        // 替换 source 字段为目标内容
        val sourceObject = JsonObject()
        sourceObject.addProperty("http", tarFilePath)
        sourceObject.addProperty("type", "tgz")
        jsonObject.add("source", sourceObject)
        return jsonObject.toString()
    }


    // 解析 Podspec 内容并返回 Source 对象
    fun parseSourceFromContent(content: String, contentType: PodSpecType): ArchiveModifier.Podspec? {
        return when (contentType) {
            PodSpecType.POD_SPEC -> parsePodspecSource(content)
            PodSpecType.JSON -> parsePodspecJsonSource(content)
        }
    }

    // 解析 .podspec 文件的 s.source
    private fun parsePodspecSource(podspecContent: String): ArchiveModifier.Podspec? {
        val lines = podspecContent.lines()
        var name: String? = null
        var version: String? = null
        var type: String? = null
        var url: String? = null
        var gitTag: String? = null
        for (line in lines) {
            val sourceLine = line.trim()
            val key = line.substringBefore("=")
            if (key.contains(".name")) {
                name = getValueFromPodspecLine(line) ?: break
            }
            if (key.contains(".version")) {
                version = getValueFromPodspecLine(line) ?: break
            }

            // 如果是 HTTP 源
            if (sourceLine.contains(":http")) {
                type = PodSpecSourceType.HTTP.name
                url = getByRegex("http", sourceLine)
            } else {
                // 如果是 Git 源
                if (sourceLine.contains(":git")) {
                    type = PodSpecSourceType.GIT.name
                    url = getByRegex("git", sourceLine)
                }
                if (sourceLine.contains(":tag")) {
                    gitTag = getByRegex("tag", sourceLine)
                }
            }
        }

        //tag可能是v#{s.version.to_s}这样的 Ruby 语言中的一个 字符串插值表达式
        if (gitTag?.contains("version") == true) {
            resolveTag(gitTag, version ?: gitTag).also { gitTag = it }
        }

        // 如果没有找到有效的 source 信息，则返回 null
        return when {
            name != null && version != null && type != null && url != null -> {
                ArchiveModifier.Podspec(name, version, Source(type, url, gitTag), null)
            }
            else -> {
                null
            }
        }

    }

    // 解析 podspec.json 文件的 s.source
    private fun parsePodspecJsonSource(podspecJsonContent: String): ArchiveModifier.Podspec? {
        val jsonObject = JsonParser.parseString(podspecJsonContent).asJsonObject
        val name = jsonObject.get("name")?.asString
        val version = jsonObject.get("version")?.asString
        val sourceJson = jsonObject.getAsJsonObject("source")

        var url = sourceJson?.get("git")?.asString
        val source = if (StringUtils.isNotBlank(url)) {
            val tag = sourceJson?.get("tag")?.asString
            Source(PodSpecSourceType.GIT.name, url!!, tag)
        } else {
            url = sourceJson.get("http")?.asString
            Source(PodSpecSourceType.HTTP.name, url.toString(), null)
        }
        return if (name != null && version != null && url != null) {
            ArchiveModifier.Podspec(name, version, source, null)
        } else {
            null
        }
    }

    fun extractNameFromPodspec(podspecContent: String): String? {
        return podspecContent.lines().find { it.contains(".name") }.let {
            getValueFromPodspecLine(it)
        }
    }

    fun extractNameFromPodspecJson(content: String): String? {
        val jsonObject = JsonParser.parseString(content).asJsonObject
        return jsonObject.get("name").asString
    }

    private fun getValueFromPodspecLine(line: String?): String? {
        return line?.split("=")?.last()?.trim()?.removeSurrounding("\"")?.removeSurrounding("'")
    }

    private fun getByRegex(key: String, line: String): String? {
        val urlMatch = Regex(":$key\\s*=>\\s*['\"]([^'\"]+)['\"]").find(line)
        return urlMatch?.groupValues?.get(1)
    }

    private fun resolveTag(template: String, version: String): String {
        // 使用正则表达式来替换 `#{}` 中的内容
        val regex = Regex("""#\{([^}]+)}""")
        return regex.replace(template, version)
    }
}
