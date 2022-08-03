package com.tencent.bkrepo.scanner.pojo

/**
 * 扫描状态
 */
@Deprecated(
    "仅用于兼容旧接口",
    replaceWith = ReplaceWith("ScanTaskStatus")
)
enum class ScanStatus {
    RUNNING,
    SUCCESS,
    STOP,
    // 未设置质量规则
    UN_QUALITY,
    // 质量规则通过
    QUALITY_PASS,
    // 扫描异常
    FAILED,
    // 质量规则未通过
    QUALITY_UNPASS,
    // 等待扫描
    INIT
}
