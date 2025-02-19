package com.tencent.bkrepo.common.metadata.cpack.dao

import com.tencent.bkrepo.common.metadata.condition.SyncCondition
import com.tencent.bkrepo.common.mongo.dao.simple.SimpleMongoDao
import com.tencent.bkrepo.common.metadata.model.TPackageAccessRule
import org.springframework.context.annotation.Conditional
import org.springframework.stereotype.Repository

@Repository
@Conditional(SyncCondition::class)
class PackageAccessRuleDao : SimpleMongoDao<TPackageAccessRule>()
