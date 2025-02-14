package com.tencent.bkrepo.cocoapods.model

import org.springframework.data.mongodb.core.index.CompoundIndex
import org.springframework.data.mongodb.core.index.CompoundIndexes
import org.springframework.data.mongodb.core.mapping.Document

@Document("cocoapods_remote_package")
@CompoundIndexes(
    CompoundIndex(
        name = "cocoapods_remote_package_idx",
        def = "{'projectId': 1, 'repoName': 1, 'packageName': 1, 'version': 1, 'packageVersion': 1}",
        background = true,
        unique = true
    )
)
data class TCocoapodsRemotePackage(
    var id: String? = null,
    var projectId: String,
    var repoName: String,
    var packageName: String,
    var packageVersion: String,
    var source: Source,

){
    data class Source(
        var type: String, //url类型 （HTTP、GIT）
        var url: String, //源仓库包的resource地址
        var gitTag: String? = null, //git仓库的tag
    )
}
