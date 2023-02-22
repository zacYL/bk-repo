package com.tencent.bkrepo.common.service.pojo

data class Release(
    val version: String,
    val majorVersion: String,
    val minorVersion: String,
    val fixVersion: String,
    val buildTime: String,
    val description: String,
    val cicd: String,
    val latestCommitId: String
)
