package com.tencent.bkrepo.scanner.pojo.request

import com.tencent.bkrepo.scanner.pojo.enums.RuleType

data class Rule(
    val type: RuleType,
    val value: String
)