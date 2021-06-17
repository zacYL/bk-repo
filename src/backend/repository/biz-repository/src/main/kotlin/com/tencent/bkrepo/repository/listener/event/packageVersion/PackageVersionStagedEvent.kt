package com.tencent.bkrepo.repository.listener.event.packageVersion

import com.tencent.bkrepo.repository.pojo.log.OperateType

class PackageVersionStagedEvent constructor(
    override val projectId: String,
    override val repoName: String,
    override val packageKey: String,
    override val version: String?,
    override val operator: String
) : PackageVersionEvent(projectId, repoName, packageKey, version, operator) {

    override fun getOperateType(): OperateType = OperateType.STAGE

}