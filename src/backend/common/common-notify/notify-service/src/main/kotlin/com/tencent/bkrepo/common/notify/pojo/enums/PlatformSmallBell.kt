package com.tencent.bkrepo.common.notify.pojo.enums

/**
 * @author tatsu
 * @date 2024/10/17
 */
enum class PlatformSmallBell(val cn: String, val en: String) {
    ARTIFACT_SCAN_STOPPED_TEMPLATE(
        "【系统提醒】制品扫描方案【\${name}】已中止",
        "[System Notice]The artifact scanning task [\${name}] has been stopped."
    ),
    ARTIFACT_SCAN_FINISH_TEMPLATE(
    "【系统提醒】制品扫描方案【\${name}】已经结束",
        "[System Notice]The artifact scanning task [\${name}] has been completed."
    ),
    ARTIFACT_REPLICA_FINISH_TEMPLATE(
        "【系统提醒】制品分发计划【\${name}】执行成功",
        "[System Notice]The artifact distribution task [\${name}] has been completed."
    ),
    ARTIFACT_REPLICA_FAIL_TEMPLATE(
        "【系统提醒】制品分发计划【\${name}】失败",
        "[System Notice]The artifact distribution task [\${name}] failed to be executed."
    ),
    ARTIFACT_REPLICA_START_TEMPLATE(
        "【系统提醒】制品分发计划【\${name}】已开始执行",
        "[System Notice]The artifact distribution task [\${name}] has begun execution."
    );
}