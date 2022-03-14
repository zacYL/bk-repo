package com.tencent.bkrepo.scanner.dao

import com.tencent.bkrepo.common.mongo.dao.simple.SimpleMongoDao
import com.tencent.bkrepo.scanner.model.TPlanRecord
import org.springframework.stereotype.Repository

@Repository
class PlanRecordDao : SimpleMongoDao<TPlanRecord>() {

}
