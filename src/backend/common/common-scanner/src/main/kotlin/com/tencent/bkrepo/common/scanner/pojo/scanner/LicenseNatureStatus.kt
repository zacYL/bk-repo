package com.tencent.bkrepo.common.scanner.pojo.scanner

enum class LicenseNatureStatus(val statusName: String) {
    PASS("pass"),
    NO_PASS("noPass"),
    UNKNOWN("unknown");

    companion object {
        fun natureStatus(value: Boolean?): LicenseNatureStatus {
            return when {
                value == null -> UNKNOWN
                value -> PASS
                else -> NO_PASS
            }
        }
    }
}