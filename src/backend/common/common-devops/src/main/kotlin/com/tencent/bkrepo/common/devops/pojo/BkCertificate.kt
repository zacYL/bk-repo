package com.tencent.bkrepo.common.devops.pojo

data class BkCertificate(
    val certType: CertType,
    val value: String
)

enum class CertType(
    val value: String
) {
    USERID("bk_username"),
    USERNAME("bk_username"),
    TOKEN("bk_token")
}
