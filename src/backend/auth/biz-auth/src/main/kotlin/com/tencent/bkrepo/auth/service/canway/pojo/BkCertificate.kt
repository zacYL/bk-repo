package com.tencent.bkrepo.auth.service.canway.pojo

data class BkCertificate(
        val certType: CertType,
        val value: String
)

enum class CertType(
        val value: String
){
    USERNAME("username"),
    TOKEN("bk_token")
}
