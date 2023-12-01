package com.tencent.bkrepo.repository.pojo.storage

import java.time.LocalDateTime

/**
 * 存储信息响应
 */
data class StoragePojo(
    val path: String,
    val total: String,
    val used: String,
    val usage: String,
    val available: String,
    val time: LocalDateTime = LocalDateTime.now(),
    val message: String
)
