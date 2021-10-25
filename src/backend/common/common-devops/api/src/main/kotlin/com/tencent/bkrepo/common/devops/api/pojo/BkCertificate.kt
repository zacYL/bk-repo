package com.tencent.bkrepo.common.devops.api.pojo

data class BkCertificate(
    val certType: CertType,
    val value: String
)

enum class CertType(
    val value: String
) {
    USERID("bk_username"),
    USERNAME("username"),
    TOKEN("bk_token")
}
