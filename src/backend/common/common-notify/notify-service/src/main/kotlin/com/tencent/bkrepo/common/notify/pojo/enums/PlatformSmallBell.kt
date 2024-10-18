package com.tencent.bkrepo.common.notify.pojo.enums

/**
 * @author tatsu
 * @date 2024/10/17
 */
enum class PlatformSmallBell(val cn: String, val en: String) {
    ARTIFACT_SCAN_STOPPED_TEMPLATE(
        "【系统提醒】\${planName}方案制品扫描已中止",
        ""
    ),
    ARTIFACT_SCAN_FINISH_TEMPLATE(
    "【系统提醒】\${planName}方案制品扫描已经结束",
        ""
    ),
    ARTIFACT_REPLICA_FINISH_TEMPLATE(
        "【系统提醒】\${taskName}计划制品分发已经完成",
        ""
    ),
    ARTIFACT_REPLICA_FAIL_TEMPLATE(
        "【系统提醒】\${taskName}计划制品分发失败",
        ""
    ),
    ARTIFACT_REPLICA_START_TEMPLATE(
        "用户[\${createdBy}]开始执行制品分发",
        ""
    );
}