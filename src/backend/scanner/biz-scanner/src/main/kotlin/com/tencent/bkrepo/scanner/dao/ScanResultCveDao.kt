package com.tencent.bkrepo.scanner.dao

import com.tencent.bkrepo.common.mongo.dao.simple.SimpleMongoDao
import com.tencent.bkrepo.scanner.model.TScanResultCve
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Repository

@Repository
class ScanResultCveDao : SimpleMongoDao<TScanResultCve>() {

}
