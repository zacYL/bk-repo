package com.tencent.bkrepo.common.metadata.util

import com.tencent.bkrepo.common.api.util.EscapeUtils
import com.tencent.bkrepo.common.metadata.model.TPackageAccessRule
import com.tencent.bkrepo.repository.pojo.packages.PackageType
import org.springframework.data.mongodb.core.query.Query
import org.springframework.data.mongodb.core.query.isEqualTo
import org.springframework.data.mongodb.core.query.where

object AccessRuleQueryHelper {
    fun ruleQuery(projectId: String, type: PackageType?, pass: Boolean?, version: String?, key: String?): Query {
        return Query(where(TPackageAccessRule::projectId).`is`(projectId))
            .apply {
                if (type != null) addCriteria(where(TPackageAccessRule::packageType).isEqualTo(type.name))
                if (pass != null) addCriteria(where(TPackageAccessRule::pass).isEqualTo(pass))
                if (version != null) {
                    val escapedValue = EscapeUtils.escapeRegexExceptWildcard(version)
                    val regexPattern = escapedValue.replace("*", ".*")
                    addCriteria(where(TPackageAccessRule::version).regex(regexPattern, "i"))
                }
                if (key != null) {
                    val escapedValue = EscapeUtils.escapeRegex(key)
                    val regexPattern = escapedValue.replace("*", ".*")
                    addCriteria(where(TPackageAccessRule::key).regex(regexPattern, "i"))
                }
            }
    }
}
