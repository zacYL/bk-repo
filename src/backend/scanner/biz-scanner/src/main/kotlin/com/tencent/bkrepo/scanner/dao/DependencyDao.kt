package com.tencent.bkrepo.scanner.dao

import com.tencent.bkrepo.common.mongo.dao.simple.SimpleMongoDao
import com.tencent.bkrepo.scanner.model.TDependency
import org.springframework.data.mongodb.core.query.Criteria
import org.springframework.data.mongodb.core.query.Query
import org.springframework.stereotype.Repository

@Repository
class DependencyDao : SimpleMongoDao<TDependency>(){

    fun find(
        projectId: String,
        repoName: String,
        packageKey: String,
        version: String
    ): TDependency? {
        val query = Query(
            Criteria.where(TDependency::projectId.name).`is`(projectId)
                .and(TDependency::repoName.name).`is`(repoName)
                .and(TDependency::packageKey.name).`is`(packageKey)
                .and(TDependency::version.name).`is`(version)
        )
        return this.findOne(query)
    }
}
