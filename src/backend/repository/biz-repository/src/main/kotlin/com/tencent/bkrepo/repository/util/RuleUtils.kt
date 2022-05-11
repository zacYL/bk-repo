package com.tencent.bkrepo.repository.util

import com.tencent.bkrepo.common.api.exception.ErrorCodeException
import com.tencent.bkrepo.common.api.message.CommonMessageCode
import com.tencent.bkrepo.common.query.enums.OperationType
import com.tencent.bkrepo.common.query.model.Rule
import java.util.regex.Pattern
import java.util.regex.PatternSyntaxException

object RuleUtils {
    fun checkRuleRegex(queryRule: Rule.QueryRule) {
        try {
            if (queryRule.operation == OperationType.REGEX)
                Pattern.compile(queryRule.value.toString())
        }catch (e: PatternSyntaxException){
            throw ErrorCodeException(CommonMessageCode.REGEX_EXPRESSION_PATTERN_ERROR,queryRule.value.toString())
        }
    }
}