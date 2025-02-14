package com.tencent.bkrepo.common.metadata.search.versions

import com.tencent.bkrepo.common.metadata.search.common.MetadataRuleInterceptor
import com.tencent.bkrepo.common.metadata.search.common.ModelValidateInterceptor
import com.tencent.bkrepo.common.metadata.search.common.SelectFieldInterceptor
import com.tencent.bkrepo.common.query.builder.MongoQueryInterpreter
import com.tencent.bkrepo.common.query.interceptor.QueryContext
import com.tencent.bkrepo.common.query.model.QueryModel
import org.springframework.data.mongodb.core.query.Query
import org.springframework.stereotype.Component
import javax.annotation.PostConstruct

@Component
class PackageVersionSearchInterpreter(
    private val metadataRuleInterceptor: MetadataRuleInterceptor
) : MongoQueryInterpreter() {

    @PostConstruct
    fun init() {
        addModelInterceptor(ModelValidateInterceptor())
        addModelInterceptor(SelectFieldInterceptor())
        addRuleInterceptor(metadataRuleInterceptor)
    }

    override fun initContext(queryModel: QueryModel, mongoQuery: Query): QueryContext {
        return PackageVersionQueryContext(queryModel, false, mongoQuery, this)
    }
}
