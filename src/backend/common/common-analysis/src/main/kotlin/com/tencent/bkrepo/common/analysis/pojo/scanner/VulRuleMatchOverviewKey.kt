package com.tencent.bkrepo.common.analysis.pojo.scanner

enum class VulRuleMatchOverviewKey(val key: String) {
    CRITICAL_IGNORE_COUNT("criticalIgnoreCount"),
    HIGH_IGNORE_COUNT("highIgnoreCount"),
    MEDIUM_IGNORE_COUNT("mediumIgnoreCount"),
    LOW_IGNORE_COUNT("lowIgnoreCount"),
    CRITICAL_FORBID_COUNT("criticalForbidCount"),
    HIGH_FORBID_COUNT("highForbidCount"),
    MEDIUM_FORBID_COUNT("mediumForbidCount"),
    LOW_FORBID_COUNT("lowForbidCount"),
    ;

    companion object {
        fun overviewKeyOf(pass: Boolean, levelName: String): String {
            val level = Level.values().first { it.levelName == levelName }
            return when (level) {
                Level.CRITICAL -> if (pass) CRITICAL_IGNORE_COUNT.key else CRITICAL_FORBID_COUNT.key
                Level.HIGH -> if (pass) HIGH_IGNORE_COUNT.key else HIGH_FORBID_COUNT.key
                Level.MEDIUM -> if (pass) MEDIUM_IGNORE_COUNT.key else MEDIUM_FORBID_COUNT.key
                Level.LOW -> if (pass) LOW_IGNORE_COUNT.key else LOW_FORBID_COUNT.key
            }
        }

        fun blacklistKeys() =
            listOf(CRITICAL_FORBID_COUNT, HIGH_FORBID_COUNT, MEDIUM_FORBID_COUNT, LOW_FORBID_COUNT)
    }
}
