package com.tencent.bkrepo.cocoapods.dao

import com.tencent.bkrepo.cocoapods.model.TCocoapodsGitInstance
import com.tencent.bkrepo.common.mongo.dao.simple.SimpleMongoDao
import org.springframework.data.mongodb.core.query.Query
import org.springframework.data.mongodb.core.query.isEqualTo
import org.springframework.data.mongodb.core.query.where
import org.springframework.stereotype.Repository

@Repository
class CocoapodsGitInstanceDao: SimpleMongoDao<TCocoapodsGitInstance>() {
    fun findByUrl(url: String): TCocoapodsGitInstance? {
        return findOne(Query(where(TCocoapodsGitInstance::url).isEqualTo(url)))
    }

    fun saveIfNotExist(gitInstance: TCocoapodsGitInstance): TCocoapodsGitInstance {
        return findByUrl(gitInstance.url) ?: save(gitInstance)
    }
}
