package com.tencent.bkrepo.repository.listener.event.packageVersion

import com.tencent.bkrepo.repository.pojo.log.OperateType
import com.tencent.bkrepo.repository.pojo.packages.PackageType

class PackageVersionDownloadEvent constructor(
    override val projectId: String,
    override val repoName: String,
    override val repoType: PackageType,
    override val packageKey: String,
    override val packageName: String,
    override val packageVersion: String,
    override val operator: String
) : PackageVersionEvent(projectId, repoName, repoType, packageKey, packageName, packageVersion, operator) {

    override fun getOperateType(): OperateType = OperateType.DOWNLOAD
}
