package com.tencent.bkrepo.common.checker.message

import com.tencent.bkrepo.common.api.message.MessageCode

/**
 * 扫描相关错误码
 */
enum class ScanMessageCode(private val key: String) : MessageCode {
    PLAN_NOT_FOUND("扫描方案不存在"),
    ARTIFACT_NOT_FOUND("所选扫描范围未匹配到制品，请重设扫描范围"),
    REPEAT_SCAN_ARTIFACT("所选扫描范围中已有制品正在扫描，请重设范围或等待扫描完成"),
    RUNNING_PLAN_DEL("方案正在执行扫描，不能删除")
    ;

    override fun getBusinessCode() = ordinal + 1
    override fun getKey() = key
    override fun getModuleCode() = 10
}
