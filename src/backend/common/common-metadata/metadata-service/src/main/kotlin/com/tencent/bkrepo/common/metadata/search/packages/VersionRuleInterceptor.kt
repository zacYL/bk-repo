package com.tencent.bkrepo.common.metadata.search.packages

import com.tencent.bkrepo.common.metadata.dao.packages.PackageVersionDao
import com.tencent.bkrepo.common.mongo.dao.AbstractMongoDao
import com.tencent.bkrepo.common.mongo.dao.util.Pages
import com.tencent.bkrepo.common.query.enums.OperationType
import com.tencent.bkrepo.common.query.interceptor.QueryContext
import com.tencent.bkrepo.common.query.interceptor.QueryRuleInterceptor
import com.tencent.bkrepo.common.query.model.Rule
import org.springframework.data.mongodb.core.query.Criteria
import org.springframework.data.mongodb.core.query.Query

abstract class VersionRuleInterceptor(
    open val packageVersionDao: PackageVersionDao
) : QueryRuleInterceptor {

    abstract fun getVersionCriteria(rule: Rule, context: PackageQueryContext): Criteria

    override fun intercept(rule: Rule, context: QueryContext): Criteria {
        require(context is PackageQueryContext)
        val versionQuery = Query(getVersionCriteria(rule, context))
        val versionMap = queryRecords(versionQuery) { query -> packageVersionDao.find(query) }
            .groupBy({ it.packageId }, { it.name })
        for ((key, value) in versionMap) {
            context.matchedVersions.putIfAbsent(key, value.toMutableSet())?.retainAll(value.toSet())
        }
        val emptyKeys = context.matchedVersions.filterValues { it.isEmpty() }.keys
        val packageIdList = (versionMap.keys - emptyKeys).toList()
        val newRule = if (packageIdList.size == 1) {
            Rule.QueryRule(ID, packageIdList.first(), OperationType.EQ)
        } else {
            Rule.QueryRule(ID, packageIdList, OperationType.IN)
        }
        return context.interpreter.resolveRule(newRule.toFixed(), context)
    }

    protected fun <T> queryRecords(query: Query, execFind: (Query) -> List<T>): List<T> {
        val records = mutableListOf<T>()
        var pageNumber = 1
        do {
            val pageRequest = Pages.ofRequest(pageNumber, PAGE_SIZE)
            val page = execFind(query.with(pageRequest))
            records.addAll(page)
            pageNumber++
        } while (page.size == PAGE_SIZE)
        return records
    }

    companion object {
        private const val ID = AbstractMongoDao.ID
        const val PAGE_SIZE = 1000
    }
}
