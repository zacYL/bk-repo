package com.tencent.bkrepo.common.metadata.search.versions

import com.tencent.bkrepo.common.metadata.search.common.CommonQueryContext
import com.tencent.bkrepo.common.query.builder.MongoQueryInterpreter
import com.tencent.bkrepo.common.query.model.QueryModel
import org.springframework.data.mongodb.core.query.Query

class PackageVersionQueryContext(
    override var queryModel: QueryModel,
    override var permissionChecked: Boolean = false,
    override val mongoQuery: Query,
    override val interpreter: MongoQueryInterpreter
) : CommonQueryContext(queryModel, permissionChecked, mongoQuery, interpreter)
