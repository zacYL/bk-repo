package com.tencent.bkrepo.repository.dao

import com.tencent.bkrepo.common.metadata.util.PackageQueryHelper
import com.tencent.bkrepo.common.mongo.dao.simple.SimpleMongoDao
import com.tencent.bkrepo.repository.model.TPackageUploadRecord
import org.springframework.data.mongodb.core.query.Query
import org.springframework.stereotype.Repository

/**
 * 包上传统计数据访问层
 */
@Repository
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
