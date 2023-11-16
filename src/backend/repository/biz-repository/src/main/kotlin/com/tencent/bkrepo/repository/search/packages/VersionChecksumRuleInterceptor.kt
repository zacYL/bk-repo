package com.tencent.bkrepo.repository.search.packages

import com.tencent.bkrepo.common.api.exception.ErrorCodeException
import com.tencent.bkrepo.common.api.message.CommonMessageCode
import com.tencent.bkrepo.common.artifact.pojo.RepositoryType
import com.tencent.bkrepo.common.query.enums.OperationType
import com.tencent.bkrepo.common.query.model.Rule
import com.tencent.bkrepo.common.security.util.SecurityUtils
import com.tencent.bkrepo.repository.dao.NodeDao
import com.tencent.bkrepo.repository.dao.PackageVersionDao
import com.tencent.bkrepo.repository.model.TNode
import com.tencent.bkrepo.repository.model.TPackageVersion
import com.tencent.bkrepo.repository.pojo.repo.RepoListOption
import com.tencent.bkrepo.repository.service.repo.RepositoryService
import org.springframework.data.mongodb.core.query.Criteria
import org.springframework.data.mongodb.core.query.Query
import org.springframework.data.mongodb.core.query.inValues
import org.springframework.data.mongodb.core.query.isEqualTo
import org.springframework.stereotype.Component

/**
 * 版本Checksum规则拦截器, 查询包版本Checksum需要在嵌套查询规则列表中指定projectId和repoType条件，且均为EQ操作
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
            val projectId = context.findProjectId(rule)
            val repoType = context.findRepoType(rule).toUpperCase()
            val userId = SecurityUtils.getUserId()
            val repoList = if (repoType in listOf(RepositoryType.DOCKER.name, RepositoryType.OCI.name)) {
                val dockerRepoList = repositoryService.listPermissionRepo(
                    userId, projectId, RepoListOption(type = RepositoryType.DOCKER.name)
                )
                val ociRepoList = repositoryService.listPermissionRepo(
                    userId, projectId, RepoListOption(type = RepositoryType.OCI.name)
                )
                dockerRepoList + ociRepoList
            } else
                repositoryService.listPermissionRepo(userId, projectId, RepoListOption(type = repoType))
            val nodeQuery = Query(
                Criteria.where(TNode::projectId.name).isEqualTo(projectId)
                    .and(TNode::repoName.name).inValues(repoList.map { it.name })
                    .and(field).isEqualTo(value.toString())
            )
            val fullPaths = queryRecords(nodeQuery) { q -> nodeDao.find(q) }.map { it.fullPath }
            return if (repoType == RepositoryType.DOCKER.name || repoType == RepositoryType.OCI.name) {
                Criteria.where(TPackageVersion::manifestPath.name).inValues(fullPaths)
            } else {
                Criteria.where(TPackageVersion::artifactPath.name).inValues(fullPaths)
            }
        }
    }

    companion object {
        private val CHECKSUM_FIELDS = arrayOf(
            TNode::sha256.name,
            TNode::md5.name
        )
    }
}
