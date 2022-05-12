package com.tencent.bkrepo.repository.util

import com.tencent.bkrepo.common.api.exception.ErrorCodeException
import com.tencent.bkrepo.common.api.message.CommonMessageCode
import com.tencent.bkrepo.common.query.enums.OperationType
import com.tencent.bkrepo.common.query.model.Rule
import java.util.regex.Pattern
import java.util.regex.PatternSyntaxException

object RuleUtils {
    val separator = "/"
    /**
     * 检查 rule 中的正则表达式格式
     */
    fun checkRuleRegex(queryRule: Rule.QueryRule) {
        try {
            if (queryRule.operation == OperationType.REGEX)
                Pattern.compile(queryRule.value.toString())
        } catch (e: PatternSyntaxException) {
            throw ErrorCodeException(CommonMessageCode.REGEX_EXPRESSION_PATTERN_ERROR, queryRule.value.toString())
        }
    }

    /**
     * Generic 仓库清理中目录要求填写【全路径】
     * 检查 rule 中目录是否以【/】开头和结尾
     */
    fun checkRuleDir(queryRule: Rule.QueryRule) {
        var value = queryRule.value.toString()
        if (queryRule.field == "fullPath") {
            val startsWith = value.startsWith(separator)
            val endsWith = value.endsWith(separator)
            if (!startsWith) {
                value = "$separator$value"
            }
            if (!endsWith){
                value = "$value$separator"
            }
            queryRule.value = value
        }
    }

    /**
     * fullPath 转化为 【前缀匹配正则表达式】
     */
    fun fullPathToRegex(queryRule: Rule.QueryRule) {
        var value = queryRule.value.toString()
        if (queryRule.field == "fullPath") {
            value = "^$value"
            queryRule.value = value
        }
    }

    /**
     * 将 rule 规则中的 fullPath 条件转化为 【前缀匹配正则表达式】
     */
    fun ruleFullPathToRegex(rule: Rule) {
        if (rule is Rule.NestedRule && rule.rules.isNotEmpty()) {
            if (!rule.toString().contains("fullPath")) return
            rule.rules.forEach {
                when (it) {
                    is Rule.NestedRule -> ruleFullPathToRegex(it)
                    is Rule.QueryRule -> {
                        fullPathToRegex(it)
                    }
                    is Rule.FixedRule -> {
                        fullPathToRegex(it.wrapperRule)
                    }
                }
            }
        }
    }
}