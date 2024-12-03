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

import com.google.gson.JsonObject
import com.google.gson.JsonParser

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
                    val indent = lines[startIndex!!].substring(0, lines[startIndex].indexOf(lines[startIndex].trimStart()))
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

    fun extractNameFromPodspec(podspecContent: String): String? {
        return podspecContent.lines().find { it.contains(".name") }?.split("=")
            ?.last()?.trim()?.removeSurrounding("\"")?.removeSurrounding("'")
    }

    fun extractNameFromPodspecJson(content: String): String? {
        val jsonObject = JsonParser.parseString(content).asJsonObject
        return jsonObject.get("name").asString
    }
}
