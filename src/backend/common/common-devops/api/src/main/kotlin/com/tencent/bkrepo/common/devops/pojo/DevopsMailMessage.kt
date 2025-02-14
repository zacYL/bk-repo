package com.tencent.bkrepo.common.devops.pojo

data class DevopsMailMessage(
    val receivers: Set<String>,
    val cc: Set<String>? = setOf(),
    val bcc: Set<String>? = setOf(),
    val body: String,
    val title: String,
    val sender: String,
    val format: Int = 1
)
