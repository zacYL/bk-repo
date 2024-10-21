package com.tencent.bkrepo.common.notify.pojo.enums

/**
 * @author tatsu
 * @date 2024/10/17
 */
enum class PlatformSmallBell(val cn: String, val en: String) {
    ARTIFACT_SCAN_STOPPED_TEMPLATE(
        "【系统提醒】\${name}方案制品扫描已中止",
        "[System Notice]The artifact scanning task [\${name}] has been stopped."
    ),
    ARTIFACT_SCAN_FINISH_TEMPLATE(
    "【系统提醒】\${name}方案制品扫描已经结束",
        "[System Notice]The artifact scanning task [\${name}] has been completed."
    ),
    ARTIFACT_REPLICA_FINISH_TEMPLATE(
        "【系统提醒】\${name}计划制品分发执行成功",
        "[System Notice]The artifact distribution task [\${name}] has been completed."
    ),
    ARTIFACT_REPLICA_FAIL_TEMPLATE(
        "【系统提醒】\${name}计划制品分发失败",
        "[System Notice]The artifact distribution task [\${name}] failed to be executed."
    ),
    ARTIFACT_REPLICA_START_TEMPLATE(
        "【系统提醒】\${name}计划制品分发已开始执行",
        "[System Notice]The artifact distribution task [\${name}] has begun execution."
    );
}