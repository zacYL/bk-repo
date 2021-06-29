package com.tencent.bkrepo.repository.listener.event.packageVersion

import com.tencent.bkrepo.repository.pojo.log.OperateType
import com.tencent.bkrepo.repository.pojo.packages.request.PackageVersionCreateRequest
import org.bson.codecs.pojo.annotations.BsonIgnore

class PackageVersionUpdatedEvent constructor(@BsonIgnore val request: PackageVersionCreateRequest) :
    PackageVersionEvent(
        projectId = request.projectId,
        repoName = request.repoName,
        repoType = request.packageType,
        packageKey = request.packageKey,
        packageName = request.packageName,
        packageVersion = request.versionName,
        operator = request.createdBy
    ) {

    override fun getOperateType(): OperateType = OperateType.UPDATE

}