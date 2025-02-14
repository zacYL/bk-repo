package net.canway.devops.common.lse.pojo

data class LicenseInfo(
    val productKey: String,
    val productName: String,
    val versionType: String,
    val modules: String,
    val applyUserNumber: Int,
    val applyNodeNumber: Int,
    val validityTime: String,
    val subscribeType: String
)
