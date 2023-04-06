package com.tencent.bkrepo.analyst.dao

import com.tencent.bkrepo.analyst.model.TCveWhitelist
import com.tencent.bkrepo.common.mongo.dao.simple.SimpleMongoDao
import org.springframework.data.mongodb.core.query.Criteria
import org.springframework.data.mongodb.core.query.Query
import org.springframework.stereotype.Repository

@Repository
class CveWhitelistDao : SimpleMongoDao<TCveWhitelist>() {

    fun getByCveId(cveId: String): TCveWhitelist? {
        return findOne(Query(Criteria.where(TCveWhitelist::cveId.name).`is`(cveId)))
    }

    fun deleteByCveId(cveId: String) {
        remove(Query(Criteria.where(TCveWhitelist::cveId.name).`is`(cveId)))
    }
}
