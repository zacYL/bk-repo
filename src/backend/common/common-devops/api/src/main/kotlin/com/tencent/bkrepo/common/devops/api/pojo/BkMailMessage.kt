package com.tencent.bkrepo.common.devops.api.pojo

import com.fasterxml.jackson.annotation.JsonProperty

data class BkMailMessage(
    @JsonProperty("bk_app_code")
    val bkAppCode: String,
    @JsonProperty("bk_app_secret")
    val bkAppSecret: String,
    @JsonProperty("bk_username")
    val bkUsername: String,
    @JsonProperty("receiver__username")
    val receiverUsername: String,
    @JsonProperty("title")
    val title: String,
    @JsonProperty("content")
    val content: String,
    @JsonProperty("body_format")
    val bodyFormat: String? = "Html",
    @JsonProperty("is_content_base64")
    val isContentBase64: Boolean? = true
)
