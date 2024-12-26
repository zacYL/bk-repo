package com.tencent.bkrepo.cocoapods.model

data class TCocoapodsGitInstance(
    var id: String? = null,
    var url: String,
    var path: String,
    var ref: String, //仓库最新的引用
)
