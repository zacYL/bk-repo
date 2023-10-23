package com.tencent.bkrepo.repository.search.versions

import com.tencent.bkrepo.common.query.builder.MongoQueryInterpreter
import com.tencent.bkrepo.common.query.interceptor.QueryContext
import com.tencent.bkrepo.common.query.model.QueryModel
import com.tencent.bkrepo.repository.search.common.MetadataRuleInterceptor
import com.tencent.bkrepo.repository.search.common.ModelValidateInterceptor
import com.tencent.bkrepo.repository.search.common.SelectFieldInterceptor
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
        return PackageVersionQueryContext(queryModel, mongoQuery, this)
    }
}
