package com.tencent.bkrepo.repository.pojo.storage

enum class DirectionType(val key: String) {
    // 仓库由大到小
    REPO_SIZE_DESC("repoSizeDesc"),
    // 仓库由小到大
    REPO_SIZE_ASC("repoSizeAsc"),
    // 文件数量由多到少
    FILE_NUMBER_DESC("fileNumberDesc"),
    // 文件数量由少到多
    FILE_NUMBER_ASC("fileNumberAsc")
}
