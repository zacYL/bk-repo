package com.tencent.bkrepo.common.metadata.dao.log

import com.tencent.bkrepo.common.metadata.condition.SyncCondition
import com.tencent.bkrepo.common.metadata.model.TOperateLogMig
import com.tencent.bkrepo.common.mongo.dao.simple.SimpleMongoDao
import org.springframework.context.annotation.Conditional
import org.springframework.stereotype.Repository

@Repository
@Conditional(SyncCondition::class)
class OperateLogMigrateDao : SimpleMongoDao<TOperateLogMig>()
