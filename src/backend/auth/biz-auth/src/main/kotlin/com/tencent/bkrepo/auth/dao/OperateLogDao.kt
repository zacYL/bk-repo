package com.tencent.bkrepo.auth.dao

import com.tencent.bkrepo.auth.model.TOperateLog
import com.tencent.bkrepo.common.mongo.dao.simple.SimpleMongoDao
import org.springframework.stereotype.Repository

@Repository
class OperateLogDao : SimpleMongoDao<TOperateLog>()
