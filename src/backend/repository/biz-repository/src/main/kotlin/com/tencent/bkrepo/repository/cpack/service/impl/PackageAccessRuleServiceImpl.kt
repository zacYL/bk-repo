package com.tencent.bkrepo.repository.cpack.service.impl

import com.tencent.bkrepo.common.api.exception.ErrorCodeException
import com.tencent.bkrepo.common.api.message.CommonMessageCode
import com.tencent.bkrepo.common.api.pojo.Page
import com.tencent.bkrepo.common.mongo.dao.util.Pages
import com.tencent.bkrepo.common.query.util.MongoEscapeUtils
import com.tencent.bkrepo.common.security.util.SecurityUtils
import com.tencent.bkrepo.repository.cpack.service.PackageAccessRuleService
import com.tencent.bkrepo.repository.dao.PackageAccessRuleDao
import com.tencent.bkrepo.repository.message.RepositoryMessageCode
import com.tencent.bkrepo.repository.model.TPackageAccessRule
import com.tencent.bkrepo.repository.pojo.packages.PackageAccessRule
import com.tencent.bkrepo.repository.pojo.packages.PackageType
import com.tencent.bkrepo.repository.pojo.packages.request.PackageAccessRuleRequest
import com.tencent.bkrepo.repository.util.AccessRuleQueryHelper
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.dao.DuplicateKeyException
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.data.mongodb.core.query.Criteria
import org.springframework.data.mongodb.core.query.Query
import org.springframework.data.mongodb.core.query.and
import org.springframework.data.mongodb.core.query.isEqualTo
import org.springframework.data.mongodb.core.query.where
import org.springframework.stereotype.Service
import java.time.LocalDateTime

@Service
class PackageAccessRuleServiceImpl(
    private val packageAccessRuleDao: PackageAccessRuleDao
) : PackageAccessRuleService {

    override fun createRule(request: PackageAccessRuleRequest) {
        with(request) {
            val tPackageAccessRule = TPackageAccessRule(
                createdBy = SecurityUtils.getUserId(),
                createdDate = LocalDateTime.now(),
                projectId = projectId,
                packageType = packageType,
                key = key,
                version = version,
                versionRuleType = versionRuleType,
                pass = pass,
                expireDate = expireDate
            )
            try {
                packageAccessRuleDao.save(tPackageAccessRule)
            } catch (e: DuplicateKeyException) {
                logger.warn("duplicate package access rule [type: $packageType, key: $key," +
                    " version: $version, versionRule: $versionRuleType]"
                )
                throw ErrorCodeException(RepositoryMessageCode.ACCESS_RULE_EXISTS)
            }
            logger.info(
                "create new package access rule [type: $packageType, key: $key," +
                        " version: $version, versionRule: $versionRuleType] success"
            )
        }
    }

    override fun deleteRule(request: PackageAccessRuleRequest) {
        with(request) {
            val criteria = where(TPackageAccessRule::projectId).isEqualTo(projectId)
                .and(TPackageAccessRule::packageType).isEqualTo(packageType)
                .and(TPackageAccessRule::key).isEqualTo(key)
                .and(TPackageAccessRule::version).isEqualTo(version)
                .and(TPackageAccessRule::versionRuleType).isEqualTo(versionRuleType)
                .and(TPackageAccessRule::pass).isEqualTo(pass)
            val tPackageAccessRule = packageAccessRuleDao.findOne(Query(criteria))
                ?: throw ErrorCodeException(CommonMessageCode.RESOURCE_NOT_FOUND, request)
            packageAccessRuleDao.removeById(tPackageAccessRule.id!!)
            logger.info(
                "remove package access rule [type: $packageType, key: $key," +
                        " version: $version, versionRule: $versionRuleType] success"
            )
        }
    }

    override fun listRulePage(
        projectId: String,
        pageNumber: Int,
        pageSize: Int,
        type: PackageType?,
        key: String?,
        version: String?,
        pass: Boolean?,
    ): Page<PackageAccessRule> {
        val query = AccessRuleQueryHelper.ruleQuery(projectId, type, pass, version, key)
        val pageRequest =
            PageRequest.of(pageNumber - 1, pageSize, Sort.Direction.DESC, TPackageAccessRule::createdDate.name)
        val count = packageAccessRuleDao.count(query)
        val records = packageAccessRuleDao.find(query.with(pageRequest)).map { convert(it) }
        return Pages.ofResponse(pageRequest, count, records)
    }

    override fun getMatchedRules(projectId: String, type: String, key: String): List<PackageAccessRule> {
        val criteria = where(TPackageAccessRule::projectId).isEqualTo(projectId)
            .and(TPackageAccessRule::packageType).isEqualTo(type)
            .andOperator(
                Criteria().orOperator(
                    where(TPackageAccessRule::expireDate).isEqualTo(null),
                    where(TPackageAccessRule::expireDate).gt(LocalDateTime.now())
                ),
                if (!key.contains(":")) where(TPackageAccessRule::key).isEqualTo(key) else {
                    Criteria().orOperator(
                        where(TPackageAccessRule::key).isEqualTo(key),
                        where(TPackageAccessRule::key).isEqualTo(key.substringBefore(":") + ":*")
                    )
                }
            )
        return packageAccessRuleDao.find(Query(criteria)).map { convert(it) }
    }

    private fun convert(tPackageAccessRule: TPackageAccessRule): PackageAccessRule {
        with(tPackageAccessRule) {
            return PackageAccessRule(
                createdBy = createdBy,
                createdDate = createdDate,
                projectId = projectId,
                packageType = packageType,
                key = key,
                version = version,
                versionRuleType = versionRuleType,
                pass = pass,
                expireDate = expireDate,
            )
        }
    }

    companion object {
        val logger: Logger = LoggerFactory.getLogger(PackageAccessRuleServiceImpl::class.java)
    }
}
