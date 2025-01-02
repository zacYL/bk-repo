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

package com.tencent.bkrepo.cocoapods.ruby

import org.jruby.RubyString
import org.springframework.stereotype.Component
import java.io.ByteArrayOutputStream
import java.io.InputStream

@Component
class PodspecSupport(private val rubySupport: RubySupport) {

    /**
     * 调用 Ruby 脚本，将 .podspec 文件转换为 JSON 字符串
     * @param inputStream .podspec 文件输入流
     * @return JSON 字符串
     */
    fun convertToJson(inputStream: InputStream): String {
        val moduleName = "CocoapodsUtil"
        val methodName = "convert_podspec_to_json"
        return try {
            val module = rubySupport.getModule(moduleName)
            val arg = RubyString.newString(rubySupport.getRuntime(), getSpecStr(inputStream))
            val result = module?.callMethod(rubySupport.getRuntime().currentContext, methodName, arg)
            result?.asJavaString() ?: "{}"
        } catch (e: Exception) {
            println("Error during conversion: ${e.message}")
            "{}"
        }
    }

    /**
     * 将 InputStream 转换为字符串
     * @param inputStream 输入流
     * @return 转换后的字符串
     */
    fun getSpecStr(inputStream: InputStream): String {
        val byteArrayOutputStream = ByteArrayOutputStream()
        inputStream.use { input ->
            val buffer = ByteArray(1024)
            var bytesRead: Int
            while (input.read(buffer).also { bytesRead = it } != -1) {
                byteArrayOutputStream.write(buffer, 0, bytesRead)
            }
        }
        return byteArrayOutputStream.toString(Charsets.UTF_8.name())
    }

}
