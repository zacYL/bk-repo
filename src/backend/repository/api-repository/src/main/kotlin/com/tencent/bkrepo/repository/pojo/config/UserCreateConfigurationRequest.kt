package com.tencent.bkrepo.repository.pojo.config

data class UserCreateConfigurationRequest(
    val type: ConfigType,
    val configuration: String
)
