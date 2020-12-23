package com.tencent.bkrepo.auth.service.canway.pojo.bk

data class BkCertificate(
    val certType: CertType,
    val value: String
)

enum class CertType(
    val value: String
) {
    USERNAME("bk_username"),
    TOKEN("bk_token")
}
