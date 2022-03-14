package com.tencent.bkrepo.scanner.dao

import com.tencent.bkrepo.common.mongo.dao.simple.SimpleMongoDao
import com.tencent.bkrepo.scanner.model.TScanPlan
import org.springframework.data.mongodb.core.query.Criteria
import org.springframework.data.mongodb.core.query.Query
import org.springframework.stereotype.Repository

@Repository
class ScanPlanDao : SimpleMongoDao<TScanPlan>() {

    fun getPlan(projectId: String, planName: String?, id: String?): TScanPlan? {
        val criteria = Criteria.where(TScanPlan::projectId.name).`is`(projectId)
            .and(TScanPlan::delete.name).`is`(false)
        planName?.let { criteria.and(TScanPlan::name.name).`is`(planName) }
        id?.let { criteria.and(TScanPlan::id.name).`is`(id) }
        val query = Query(criteria)
        return this.findOne(query, TScanPlan::class.java)
    }

}
