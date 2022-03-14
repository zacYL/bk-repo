package com.tencent.bkrepo.common.checker.message

import com.tencent.bkrepo.common.api.message.MessageCode

/**
 * 扫描相关错误码
 */
enum class ScanMessageCode(private val key: String) : MessageCode {
    PLAN_NOT_FOUND("scan.plan.not-found"),
    ARTIFACT_NOT_FOUND("scan.artifact.not-match"),
    REPEAT_SCAN_ARTIFACT("repeat.scan.artifact"),
    RUNNING_PLAN_DEL("plan is running, cannot be deleted")
    ;

    override fun getBusinessCode() = ordinal + 1
    override fun getKey() = key
    override fun getModuleCode() = 10
}
