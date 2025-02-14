package com.tencent.bkrepo.common.metadata.cpack.dao

import com.tencent.bkrepo.common.mongo.dao.simple.SimpleMongoDao
import com.tencent.bkrepo.common.metadata.model.TPackageAccessRule
import org.springframework.stereotype.Repository

@Repository
class PackageAccessRuleDao : SimpleMongoDao<TPackageAccessRule>()
