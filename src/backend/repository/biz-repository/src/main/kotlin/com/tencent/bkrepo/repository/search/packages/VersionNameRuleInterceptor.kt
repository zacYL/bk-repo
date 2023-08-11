package com.tencent.bkrepo.repository.search.packages

import com.tencent.bkrepo.common.api.exception.ErrorCodeException
import com.tencent.bkrepo.common.api.message.CommonMessageCode
import com.tencent.bkrepo.common.query.enums.OperationType
import com.tencent.bkrepo.common.query.model.Rule
import com.tencent.bkrepo.repository.dao.PackageVersionDao
import com.tencent.bkrepo.repository.model.TPackageVersion
import org.springframework.data.mongodb.core.query.Criteria
import org.springframework.stereotype.Component

/**
 * 版本号规则拦截器
 */
@Component
class VersionNameRuleInterceptor(
    override val packageVersionDao: PackageVersionDao
) : VersionRuleInterceptor(packageVersionDao) {

    override fun match(rule: Rule): Boolean {
        return rule is Rule.QueryRule && rule.field == "version"
    }

    override fun getVersionCriteria(rule: Rule, context: PackageQueryContext): Criteria {
        with(rule as Rule.QueryRule) {
            val versionQueryRule = when (operation) {
                OperationType.IN ->
                    rule.copy(field = TPackageVersion::name.name, value = (value as List<*>).map { it.toString() })

                in SUPPORT_OPERATIONS -> rule.copy(field = TPackageVersion::name.name, value = value.toString())
                else -> throw ErrorCodeException(
                    CommonMessageCode.METHOD_NOT_ALLOWED,
                    "$field only support ${SUPPORT_OPERATIONS.map { it.name }} operation type."
                )
            }.toFixed()
            return context.interpreter.resolveRule(versionQueryRule, context)
        }
    }

    companion object {
        private val SUPPORT_OPERATIONS = arrayOf(
            OperationType.EQ,
            OperationType.IN,
            OperationType.MATCH,
            OperationType.MATCH_I
        )
    }
}
