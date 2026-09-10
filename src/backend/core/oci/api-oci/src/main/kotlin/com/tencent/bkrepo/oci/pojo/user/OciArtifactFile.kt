package com.tencent.bkrepo.oci.pojo.user

data class OciArtifactFile(
    val path: String,
    val digest: String,
    val size: Long,
    val mediaType: String
)
