package com.tencent.bkrepo.repository.search.packages

import com.tencent.bkrepo.common.api.constant.StringPool
import com.tencent.bkrepo.common.api.exception.ErrorCodeException
import com.tencent.bkrepo.common.api.message.CommonMessageCode
import com.tencent.bkrepo.common.query.enums.OperationType
import com.tencent.bkrepo.common.query.model.Rule
import com.tencent.bkrepo.common.security.util.SecurityUtils
import com.tencent.bkrepo.repository.dao.NodeDao
import com.tencent.bkrepo.repository.dao.PackageVersionDao
import com.tencent.bkrepo.repository.model.TNode
import com.tencent.bkrepo.repository.model.TPackageVersion
import com.tencent.bkrepo.repository.pojo.packages.PackageType
import com.tencent.bkrepo.repository.pojo.repo.RepoListOption
import com.tencent.bkrepo.repository.service.repo.RepositoryService
import org.springframework.data.mongodb.core.query.Criteria
import org.springframework.data.mongodb.core.query.Query
import org.springframework.data.mongodb.core.query.inValues
import org.springframework.data.mongodb.core.query.isEqualTo
import org.springframework.stereotype.Component

/**
 * 版本Checksum规则拦截器, 查询包版本Checksum需要在嵌套查询规则列表中指定projectId和repoType条件，且均为EQ操作
 *
 * 注意: Docker切换为Oci后需要更改这部分逻辑
 */
@Component
class VersionChecksumRuleInterceptor(
    override val packageVersionDao: PackageVersionDao,
    private val nodeDao: NodeDao,
    private val repositoryService: RepositoryService
) : VersionRuleInterceptor(packageVersionDao) {

    override fun match(rule: Rule): Boolean {
        return rule is Rule.QueryRule && rule.field in CHECKSUM_FIELDS
    }

    override fun getVersionCriteria(rule: Rule, context: PackageQueryContext): Criteria {
        with(rule as Rule.QueryRule) {
            if (operation != OperationType.EQ) {
                throw ErrorCodeException(CommonMessageCode.METHOD_NOT_ALLOWED, "$field only support EQ operation type.")
            }
            val projectId = context.findProjectId()
            val repoType = context.findRepoType()
            val userId = SecurityUtils.getUserId()
            val repoList =
                repositoryService.listPermissionRepo(userId, projectId, RepoListOption(type = repoType)).map { it.name }
            val nodeQuery = Query(
                Criteria.where(TNode::projectId.name).isEqualTo(projectId)
                    .and(TNode::repoName.name).inValues(repoList)
                    .and(field).isEqualTo(value.toString())
            )
            val fullPaths = queryRecords(nodeQuery) { q -> nodeDao.find(q) }.map { it.fullPath }
            // Docker切换为Oci后需要更改这部分逻辑
            return if (repoType.equals(PackageType.DOCKER.name, ignoreCase = true)) {
                Criteria.where(TPackageVersion::manifestPath.name).inValues(
                    fullPaths.map { it.substringBeforeLast(StringPool.SLASH) + DOCKER_MANIFEST_SUFFIX }
                )
            } else {
                Criteria.where(TPackageVersion::artifactPath.name).inValues(fullPaths)
            }
        }
    }

    companion object {
        private const val DOCKER_MANIFEST_SUFFIX = "/manifest.json"
        private val CHECKSUM_FIELDS = arrayOf(
            TNode::sha256.name,
            TNode::md5.name
        )
    }
}
