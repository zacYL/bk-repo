package com.tencent.bkrepo.repository.dao

import com.tencent.bkrepo.common.mongo.dao.simple.SimpleMongoDao
import com.tencent.bkrepo.repository.model.TPackageAccessRule
import org.springframework.stereotype.Repository

@Repository
class PackageAccessRuleDao : SimpleMongoDao<TPackageAccessRule>()
