package com.tencent.bkrepo.auth.pojo.admin

import com.fasterxml.jackson.annotation.JsonFormat
import java.time.LocalDateTime


class AdminVO(
    val id: String?,
    val userId: String,
    val username: String,
    val scopeId: String,
    val scopeCode: String,
    val createdBy: String,
    val createdTime: LocalDateTime,
    val createdByName: String,
    val updatedBy: String,
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
    val updatedTime: LocalDateTime,
    val updatedByName: String
)
