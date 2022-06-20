package com.tencent.bkrepo.common.scanner.pojo.scanner

enum class LicenseNature(val natureName: String, val level: Int) {
    UN_COMPLIANCE("unCompliance", 3),
    UN_RECOMMEND("unRecommend", 2),
    UNKNOWN("unknown", 1),
    NORMAL("normal", 0);
}
