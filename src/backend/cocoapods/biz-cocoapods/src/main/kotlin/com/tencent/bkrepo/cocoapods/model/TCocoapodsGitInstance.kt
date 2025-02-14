package com.tencent.bkrepo.cocoapods.model

import org.springframework.data.mongodb.core.index.CompoundIndex
import org.springframework.data.mongodb.core.index.CompoundIndexes
import org.springframework.data.mongodb.core.mapping.Document

@Document("cocoapods_git_instance")
@CompoundIndexes(
    CompoundIndex(
        name = "url_idx",
        def = "{'url': 1}",
        background = true,
    )
)
data class TCocoapodsGitInstance(
    var id: String? = null,
    var url: String,
    var path: String,
    var ref: String, //仓库最新的引用
)
