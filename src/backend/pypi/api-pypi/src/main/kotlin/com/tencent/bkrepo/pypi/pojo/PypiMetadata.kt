package com.tencent.bkrepo.pypi.pojo

data class PypiMetadata(
    val name: String,
    val version: String,
    val summary: String? = null,
    val homePage: String? = null,
    val generator: String? = null,
    val downloadUrl: String? = null,
    val license: String? = null,
    val metadataVersion: String? = null,
    val platform: List<String>? = null,
    val classifiers: List<String>? = null,
    val description: String? = null
)
