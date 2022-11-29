package com.tencent.bkrepo.nuget.pojo.response

import java.net.URI

data class PackageListResponse(
    val registrationUrl: URI,
    val packageId: String,
    val description: String,
    val creator: String,
    val versionsCount: Long,
    val latestVersion: String,
    val downloads: Long
)
