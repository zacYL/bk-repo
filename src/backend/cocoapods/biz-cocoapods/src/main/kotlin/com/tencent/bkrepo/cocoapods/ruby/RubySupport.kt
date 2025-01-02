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

import org.jruby.Ruby
import org.jruby.runtime.builtin.IRubyObject
import org.springframework.stereotype.Component
import java.io.File
import javax.annotation.PostConstruct

/**
 * 为cocoapods提供Ruby环境支持
 * 可以执行ruby脚本
 */
@Component
class RubySupport {
    private val rubyRuntime: Ruby = Ruby.newInstance()
    private val modulesMap: MutableMap<String, IRubyObject> = mutableMapOf()
    private val RUBY_SCRIPT_PATH: String = "src/main/resources/ruby_script"
    fun getRuntime(): Ruby = rubyRuntime

    /**
     * 遍历脚本路径下的.rb脚本
     * 将他们加载到ruby环境中
     * 由于所有脚本都被加载到一个ruby环境中
     * 为避免冲突，最好每个脚本定义一个module
     * 尽量少使用全局变量
     * 每个脚本提供moduleName来获取，也就是文件名
     * 文件名需要和module的名字一样
     */
    @PostConstruct
    fun init() {
        try {
            val modulesDir = File(RUBY_SCRIPT_PATH)
            if (!modulesDir.exists() || !modulesDir.isDirectory) {
                throw RuntimeException("Ruby modules directory not found: ${RUBY_SCRIPT_PATH}")
            }

            val rubyFiles = modulesDir.listFiles { _, name -> name.endsWith(".rb") } ?: return

            for (rubyFile in rubyFiles) {
                val moduleName = rubyFile.name.removeSuffix(".rb")
                val moduleContent = rubyFile.readText(Charsets.UTF_8)

                // 执行脚本并获取模块对象
                rubyRuntime.executeScript(moduleContent, rubyFile.name)
                val moduleObject = rubyRuntime.getModule(moduleName)

                // 存储到 modulesMap 中
                modulesMap[moduleName] = moduleObject
                println("Loaded module: '$moduleName', from: '${rubyFile.name}'")
            }
        } catch (e: Exception) {
            throw RuntimeException("Failed to initialize Ruby modules", e)
        }
    }

    fun getModule(moduleName: String): IRubyObject? = modulesMap[moduleName]
}
