package com.tencent.bkrepo.common.artifact.util

import com.tencent.bkrepo.common.artifact.util.version.SemVersionParser
import com.tencent.bkrepo.repository.pojo.packages.PackageType
import com.tencent.bkrepo.repository.pojo.packages.VersionRuleType
import org.slf4j.LoggerFactory

object PackageAccessRuleUtils {

    private val logger = LoggerFactory.getLogger(PackageAccessRuleUtils::class.java)

    fun matchRule(
        packageType: PackageType,
        version: String,
        ruleVersion: String?,
        ruleType: VersionRuleType?
    ): Boolean {
        if (ruleVersion.isNullOrBlank() || ruleType == null) return true
        if (packageType == PackageType.DOCKER || packageType == PackageType.OCI) {
            return matchNonSemVerRule(version, ruleVersion, ruleType)
        }
        return try {
            val packageSemVersion = SemVersionParser.parse(version.removePrefix("v"))
            val ruleSemVersion = SemVersionParser.parse(ruleVersion.removePrefix("v"))
            when (ruleType) {
                VersionRuleType.EQ -> packageSemVersion == ruleSemVersion
                VersionRuleType.NE -> packageSemVersion != ruleSemVersion
                VersionRuleType.GT -> packageSemVersion > ruleSemVersion
                VersionRuleType.GTE -> packageSemVersion >= ruleSemVersion
                VersionRuleType.LE -> packageSemVersion < ruleSemVersion
                VersionRuleType.LTE -> packageSemVersion <= ruleSemVersion
            }
        } catch (e: IllegalArgumentException) {
            logger.error("failed to parse version [$version] or [$ruleVersion]", e)
            matchNonSemVerRule(version, ruleVersion, ruleType)
        }
    }

    private fun matchNonSemVerRule(version: String, ruleVersion: String, ruleType: VersionRuleType): Boolean {
        return when (ruleType) {
            VersionRuleType.EQ -> version == ruleVersion
            VersionRuleType.NE -> version != ruleVersion
            else -> false
        }
    }
}
