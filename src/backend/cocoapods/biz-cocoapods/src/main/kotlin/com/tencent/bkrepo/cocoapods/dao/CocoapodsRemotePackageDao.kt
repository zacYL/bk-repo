package com.tencent.bkrepo.cocoapods.dao

import com.tencent.bkrepo.cocoapods.model.TCocoapodsRemotePackage
import com.tencent.bkrepo.common.mongo.dao.simple.SimpleMongoDao
import org.springframework.dao.DuplicateKeyException
import org.springframework.data.mongodb.BulkOperationException
import org.springframework.data.mongodb.core.BulkOperations
import org.springframework.data.mongodb.core.insert
import org.springframework.data.mongodb.core.query.Query
import org.springframework.data.mongodb.core.query.and
import org.springframework.data.mongodb.core.query.isEqualTo
import org.springframework.data.mongodb.core.query.where
import org.springframework.stereotype.Repository

@Repository
class CocoapodsRemotePackageDao : SimpleMongoDao<TCocoapodsRemotePackage>() {
    fun findOne(projectId: String, repoName: String, packageName: String, packageVersion: String): TCocoapodsRemotePackage? {
        return findOne(Query(where(TCocoapodsRemotePackage::projectId).isEqualTo(projectId)
            .and(TCocoapodsRemotePackage::repoName).isEqualTo(repoName)
            .and(TCocoapodsRemotePackage::packageName).isEqualTo(packageName)
            .and(TCocoapodsRemotePackage::packageVersion).isEqualTo(packageVersion)))
    }

    fun saveIfNotExists(list: Collection<TCocoapodsRemotePackage>) {
        try {
            determineMongoTemplate()
                .insert<TCocoapodsRemotePackage>()
                .withBulkMode(BulkOperations.BulkMode.UNORDERED)
                .bulk(list)
        } catch (ignore: DuplicateKeyException) {
        } catch (ignore: BulkOperationException) {
        }
    }
}
