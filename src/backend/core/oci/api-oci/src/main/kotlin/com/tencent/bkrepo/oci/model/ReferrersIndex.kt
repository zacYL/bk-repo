package com.tencent.bkrepo.oci.model

import com.fasterxml.jackson.annotation.JsonInclude
import com.tencent.bkrepo.oci.constant.IMAGE_INDEX_MEDIA_TYPE

data class ReferrersIndex(
    val schemaVersion: Int = 2,
    val mediaType: String = IMAGE_INDEX_MEDIA_TYPE,
    val manifests: List<ReferrerDescriptor> = emptyList()
)

@JsonInclude(JsonInclude.Include.NON_NULL)
data class ReferrerDescriptor(
    val mediaType: String,
    val size: Long,
    val digest: String,
    val artifactType: String? = null
)
