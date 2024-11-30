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

package com.tencent.bkrepo.repository.search.common

import com.tencent.bkrepo.auth.pojo.enums.PermissionAction.READ

import com.tencent.bkrepo.common.query.enums.OperationType
import com.tencent.bkrepo.common.query.interceptor.QueryContext
import com.tencent.bkrepo.common.query.interceptor.QueryRuleInterceptor
import com.tencent.bkrepo.common.query.model.Rule
import com.tencent.bkrepo.common.security.manager.PermissionManager
import com.tencent.bkrepo.common.security.util.SecurityUtils
import com.tencent.bkrepo.repository.pojo.node.NodeInfo
import org.springframework.data.mongodb.core.query.Criteria
import org.springframework.stereotype.Component

/**
 * 仓库类型规则拦截器
 *
 * 条件构造器中传入条件是`repoType`，需要转换成对应的仓库列表
 */
@Component
class PathRuleInterceptor(
    private val permissionManager: PermissionManager,
) : QueryRuleInterceptor {

    override fun match(rule: Rule): Boolean {
        return rule is Rule.QueryRule && rule.field == "path" && isSupportRule(rule)
    }

    override fun intercept(rule: Rule, context: QueryContext): Criteria {
        require(rule is Rule.QueryRule)
        with(rule) {
            require(context is CommonQueryContext)
            val projectId = context.findProjectId()
            val repoName = context.findRepoName()
            val userId = SecurityUtils.getUserId()

            val userAuthPath = permissionManager.getUserAuthPath(userId, projectId, repoName, READ)

            if (rule.operation == OperationType.EQ) {
                val path = value.toString()
                val find = userAuthPath.find { path.startsWith(it.trimEnd('/') + "/") }
                if (find != null) {
                    // 能找到父路径，则拥有全部权限
                    val queryRule = Rule.QueryRule(NodeInfo::path.name, value, OperationType.EQ)
                    return context.interpreter.resolveRule(queryRule, context)
                } else {
                    val mapNotNull = userAuthPath.mapNotNull {
                        getNextLevelPath(path, it)
                    }
                    val queryRule = Rule.QueryRule(NodeInfo::fullPath.name, mapNotNull, OperationType.IN)
                    return context.interpreter.resolveRule(queryRule, context)
                }
            } else {
                throw IllegalArgumentException("path only support EQ operation type.")
            }
        }
    }

    fun getNextLevelPath(fullPath: String, basePath: String): String? {
        val normalizedFullPath = if (fullPath.endsWith('/')) fullPath else "$fullPath/"
        val normalizedBasePath = if (basePath.endsWith('/')) basePath else "$basePath/"

        if (!normalizedFullPath.startsWith(normalizedBasePath)) {
            return null
        }

        val remainingPath = normalizedFullPath.removePrefix(normalizedBasePath)

        val nextLevelSegments = remainingPath.split('/').filter { it.isNotBlank() }
        return if (nextLevelSegments.isNotEmpty()) {
            "$normalizedBasePath${nextLevelSegments[0]}"
        } else {
            null
        }
    }

    private fun isSupportRule(rule: Rule.QueryRule): Boolean {
        with(rule) {
            return if (operation == OperationType.EQ) {
                value is List<*> && (value as List<*>).firstOrNull() is CharSequence
            } else {
                value is CharSequence
            }
        }
    }
}
