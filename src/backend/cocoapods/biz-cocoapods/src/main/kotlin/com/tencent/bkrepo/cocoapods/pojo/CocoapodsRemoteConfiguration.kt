package com.tencent.bkrepo.cocoapods.pojo

import com.tencent.bkrepo.cocoapods.pojo.enums.RemoteRepoType

class CocoapodsRemoteConfiguration(
    var type: RemoteRepoType,
    var downloadUrl: String?, //OTHER类型的下载地址
)
