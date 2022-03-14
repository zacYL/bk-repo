package com.tencent.bkrepo.scanner.exception

import com.tencent.bkrepo.common.api.exception.NotFoundException
import com.tencent.bkrepo.common.checker.message.ScanMessageCode

/**
 * 方案不存在
 */
class PlanNotFoundException(
    planId: String
) : NotFoundException(ScanMessageCode.PLAN_NOT_FOUND, planId)
