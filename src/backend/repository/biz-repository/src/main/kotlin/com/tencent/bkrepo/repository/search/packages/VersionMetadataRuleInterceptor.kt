package com.tencent.bkrepo.repository.search.packages

import com.tencent.bkrepo.common.query.model.Rule
import com.tencent.bkrepo.repository.dao.PackageVersionDao
import com.tencent.bkrepo.repository.search.common.MetadataRuleInterceptor
import org.springframework.stereotype.Component

/**
 * 版本元数据规则拦截器
 */
@Component
class VersionMetadataRuleInterceptor(
    override val packageVersionDao: PackageVersionDao,
    private val metadataRuleInterceptor: MetadataRuleInterceptor
) : VersionRuleInterceptor(packageVersionDao) {

    override fun match(rule: Rule) = metadataRuleInterceptor.match(rule)
    override fun getVersionCriteria(rule: Rule, context: PackageQueryContext) =
        metadataRuleInterceptor.intercept(rule, context)
}
