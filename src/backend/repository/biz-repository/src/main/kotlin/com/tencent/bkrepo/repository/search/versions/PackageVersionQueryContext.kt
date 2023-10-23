package com.tencent.bkrepo.repository.search.versions

import com.tencent.bkrepo.common.query.builder.MongoQueryInterpreter
import com.tencent.bkrepo.common.query.model.QueryModel
import com.tencent.bkrepo.repository.search.common.CommonQueryContext
import org.springframework.data.mongodb.core.query.Query

class PackageVersionQueryContext(
    override var queryModel: QueryModel,
    override val mongoQuery: Query,
    override val interpreter: MongoQueryInterpreter
) : CommonQueryContext(queryModel, mongoQuery, interpreter)
