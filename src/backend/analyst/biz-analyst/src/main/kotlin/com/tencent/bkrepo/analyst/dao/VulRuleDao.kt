package com.tencent.bkrepo.analyst.dao

import com.mongodb.client.result.DeleteResult
import com.tencent.bkrepo.analyst.model.TVulRule
import com.tencent.bkrepo.common.mongo.dao.simple.SimpleMongoDao
import org.springframework.data.mongodb.core.query.Criteria.where
import org.springframework.data.mongodb.core.query.Query
import org.springframework.data.mongodb.core.query.inValues
import org.springframework.data.mongodb.core.query.isEqualTo
import org.springframework.stereotype.Repository

@Repository
class VulRuleDao : SimpleMongoDao<TVulRule>() {

    fun getByVulId(vulId: String, pass: Boolean?): TVulRule? {
        val criteria = where(TVulRule::vulId.name).isEqualTo(vulId)
        if (pass != null) criteria.and(TVulRule::pass.name).isEqualTo(pass)
        return findOne(Query(criteria))
    }

    fun deleteByVulId(vulIds: Set<String>): DeleteResult {
        return remove(Query(where(TVulRule::vulId.name).inValues(vulIds)))
    }
}
