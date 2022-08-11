package com.tencent.bkrepo.scanner.utils

object CveInfoUtil {
    fun checkCveId(cveId: String): Boolean {
        return cveId.matches(Regex("^(CVE)-\\d{4}-\\d{4,}$"))
    }
}