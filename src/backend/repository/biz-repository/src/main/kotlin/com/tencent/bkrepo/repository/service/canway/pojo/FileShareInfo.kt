package com.tencent.bkrepo.repository.service.canway.pojo

data class FileShareInfo(
        val fileName: String,
        val md5: String,
        val projectId: String,
        val repoName: String,
        val downloadUrl: String,
        val qrCodeBase64: String
)