package com.tencent.bkrepo.common.metadata.dao.packages

import com.tencent.bkrepo.common.metadata.condition.SyncCondition
import com.tencent.bkrepo.common.metadata.model.TPackageUploadRecord
import com.tencent.bkrepo.common.metadata.util.PackageQueryHelper
import com.tencent.bkrepo.common.mongo.dao.simple.SimpleMongoDao
import org.springframework.context.annotation.Conditional
import org.springframework.data.mongodb.core.query.Query
import org.springframework.stereotype.Repository

/**
 * 包上传统计数据访问层
 */
@Repository
@Conditional(SyncCondition::class)
class PackageUploadsDao : SimpleMongoDao<TPackageUploadRecord>() {
    fun findByVersion(
        projectId: String,
        repoName: String,
        packageKey: String,
        versionName: String
    ): TPackageUploadRecord? {
        val criteria = PackageQueryHelper.recordCriteria(
            projectId = projectId,
            repoName = repoName,
            packageKey = packageKey,
            packageVersion = versionName
        )
        return this.findOne(Query(criteria))
    }
}
