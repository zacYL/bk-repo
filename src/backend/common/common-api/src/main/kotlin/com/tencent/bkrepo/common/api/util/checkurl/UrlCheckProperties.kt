package com.tencent.bkrepo.common.api.util.checkurl

data class UrlCheckProperties(
    var schemes: List<String> = listOf("http", "https"),
    /** 主机名白名单，见 [mode]；空则只校验协议和域名格式，与现网默认行为一致 */
    var rules: List<String> = emptyList(),
    var mode: String = "subhost",
)
