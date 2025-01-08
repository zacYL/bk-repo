package com.tencent.bkrepo.common.artifact.repository.core

import com.tencent.bkrepo.auth.pojo.enums.PermissionAction
import com.tencent.bkrepo.common.api.constant.HttpStatus
import com.tencent.bkrepo.common.api.exception.ErrorCodeException
import com.tencent.bkrepo.common.api.util.Preconditions
import com.tencent.bkrepo.common.artifact.api.ArtifactInfo
import com.tencent.bkrepo.common.artifact.constant.ARTIFACT_INFO_KEY
import com.tencent.bkrepo.common.artifact.constant.FORBID_STATUS
import com.tencent.bkrepo.common.artifact.constant.LOCK_STATUS
import com.tencent.bkrepo.common.artifact.constant.RESERVED_KEY
import com.tencent.bkrepo.common.artifact.exception.NodeNotFoundException
import com.tencent.bkrepo.common.artifact.exception.RepoNotFoundException
import com.tencent.bkrepo.common.artifact.exception.VersionConflictException
import com.tencent.bkrepo.common.artifact.exception.VersionNotFoundException
import com.tencent.bkrepo.common.artifact.message.ArtifactMessageCode
import com.tencent.bkrepo.common.artifact.pojo.PackageVersionInfo
import com.tencent.bkrepo.common.artifact.pojo.RepositoryCategory
import com.tencent.bkrepo.common.artifact.pojo.request.PackageVersionMoveCopyRequest
import com.tencent.bkrepo.common.artifact.repository.context.ArtifactContextHolder
import com.tencent.bkrepo.common.artifact.repository.local.LocalRepository
import com.tencent.bkrepo.common.artifact.util.PackageKeys
import com.tencent.bkrepo.common.security.manager.PermissionManager
import com.tencent.bkrepo.common.security.util.SecurityUtils
import com.tencent.bkrepo.common.service.util.HttpContextHolder
import com.tencent.bkrepo.repository.api.NodeClient
import com.tencent.bkrepo.repository.api.PackageAccessRuleClient
import com.tencent.bkrepo.repository.api.PackageClient
import com.tencent.bkrepo.repository.api.RepositoryClient
import com.tencent.bkrepo.repository.constant.CoverStrategy
import com.tencent.bkrepo.repository.pojo.node.NodeDetail
import com.tencent.bkrepo.repository.pojo.node.service.NodeDeleteRequest
import com.tencent.bkrepo.repository.pojo.node.service.NodeMoveCopyRequest
import com.tencent.bkrepo.repository.pojo.packages.PackageVersion
import com.tencent.bkrepo.repository.pojo.packages.request.PackageVersionCreateRequest
import com.tencent.bkrepo.repository.pojo.repo.RepositoryDetail
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired

abstract class ArtifactExtService : ArtifactService() {

    @Autowired
    lateinit var nodeClient: NodeClient

    @Autowired
    lateinit var packageClient: PackageClient

    @Autowired
    lateinit var repositoryClient: RepositoryClient

    @Autowired
    lateinit var permissionManager: PermissionManager

    @Autowired
    lateinit var packageAccessRuleClient: PackageAccessRuleClient

    /**
     * 查询版本信息
     */
    abstract fun getVersionDetail(userId: String, artifactInfo: ArtifactInfo): PackageVersionInfo

    /**
     * 删除包
     */
    abstract fun deletePackage(userId: String, artifactInfo: ArtifactInfo)

    /**
     * 删除包版本
     */
    abstract fun deleteVersion(userId: String, artifactInfo: ArtifactInfo)

    /**
     * 移动/复制版本
     */
    open fun moveCopyVersion(request: PackageVersionMoveCopyRequest, move: Boolean) {
        with(request) {
            permissionManager.checkRepoPermission(PermissionAction.READ, srcProjectId, srcRepoName)
            permissionManager.checkRepoPermission(PermissionAction.WRITE, dstProjectId, dstRepoName)
            val (srcRepo, dstRepo) = getAndCheckRepository(srcProjectId, srcRepoName, dstProjectId, dstRepoName)

            // 检查源制品锁定/禁用状态
            val srcVersion = getAndCheckSrcVersion(srcProjectId, srcRepoName, packageKey, version, move)
            // 检查目标仓库覆盖策略/被覆盖制品锁定状态
            checkDst(dstRepo, packageKey, version, overwrite)

            copyNodes(
                srcProjectId, srcRepoName, dstProjectId, dstRepoName, packageKey, version,
                srcVersion.manifestPath, srcVersion.contentPath
            )
            updateIndex(dstProjectId, dstRepoName, packageKey, version)
            val srcPackage = packageClient.findPackageByKey(srcProjectId, srcRepoName, packageKey).data!!
            packageClient.createVersion(
                PackageVersionCreateRequest(
                    projectId = dstProjectId,
                    repoName = dstRepoName,
                    packageName = srcPackage.name,
                    packageKey = packageKey,
                    packageType = srcPackage.type,
                    packageDescription = srcPackage.description,
                    packageExtension = srcPackage.extension,
                    versionName = version,
                    size = srcVersion.size,
                    manifestPath = srcVersion.manifestPath,
                    artifactPath = srcVersion.contentPath,
                    packageMetadata = srcVersion.packageMetadata.filter { it.key !in RESERVED_KEY && it.system },
                    tags = srcVersion.tags,
                    extension = srcVersion.extension,
                    overwrite = true,
                    createdBy = SecurityUtils.getPrincipal()
                )
            )
            logger.info(
                "success to copy version [$packageKey/$version] " +
                        "from [$srcProjectId/$srcRepoName] to [$dstProjectId/$dstRepoName]"
            )
            if (move) {
                ArtifactContextHolder.setRepoDetail(srcRepo)
                val versionDeleteInfo = buildVersionDeleteArtifactInfo(srcProjectId, srcRepoName, packageKey, version)
                HttpContextHolder.getRequest().setAttribute(ARTIFACT_INFO_KEY, versionDeleteInfo)
                logger.info("prepare to remove [$packageKey/$version] from [$srcProjectId/$srcRepoName]")
                deleteVersion(SecurityUtils.getPrincipal(), versionDeleteInfo)
            }
        }
    }

    /**
     * 更新共享索引文件
     */
    protected open fun updateIndex(
        projectId: String,
        repoName: String,
        packageKey: String,
        version: String
    ) {
        (ArtifactContextHolder.getRepository(RepositoryCategory.LOCAL) as LocalRepository)
            .updateIndex(projectId, repoName, packageKey, version)
    }

    /**
     * 构建用于删除包版本的artifactInfo
     */
    protected open fun buildVersionDeleteArtifactInfo(
        projectId: String,
        repoName: String,
        packageKey: String,
        version: String
    ): ArtifactInfo {
        throw NotImplementedError()
    }

    @Suppress("LongParameterList")
    private fun getArtifactNodes(
        projectId: String,
        repoName: String,
        packageKey: String,
        version: String,
        manifestPath: String?,
        artifactPath: String?
    ): List<NodeDetail> {
        return (ArtifactContextHolder.getRepository(RepositoryCategory.LOCAL) as LocalRepository)
            .getArtifactFullPaths(projectId, repoName, packageKey, version, manifestPath, artifactPath)
            .also {
                logger.info("artifactFullPaths of [$projectId/$repoName/$packageKey/$version]: [$it]")
                require(it.isNotEmpty())
            }.map {
                nodeClient.getNodeDetail(projectId, repoName, it).data
                    ?: throw NodeNotFoundException("$projectId/$repoName/$it")
            }
    }

    @Suppress("LongParameterList")
    private fun copyNodes(
        srcProjectId: String,
        srcRepoName: String,
        dstProjectId: String,
        dstRepoName: String,
        packageKey: String,
        version: String,
        manifestPath: String?,
        artifactPath: String?
    ) {
        val operator = SecurityUtils.getPrincipal()
        getArtifactNodes(srcProjectId, srcRepoName, packageKey, version, manifestPath, artifactPath).forEach {
            // 目录/文件 -> 已存在的目录: 作为已存在目录的子目录/文件
            val dstNode = nodeClient.getNodeDetail(dstProjectId, dstRepoName, it.fullPath).data
            if (dstNode?.folder == true) nodeClient.deleteNode(
                NodeDeleteRequest(
                    projectId = dstProjectId,
                    repoName = dstRepoName,
                    fullPath = it.fullPath,
                    operator = operator
                )
            )
            nodeClient.copyNode(
                NodeMoveCopyRequest(
                    srcProjectId = srcProjectId,
                    srcRepoName = srcRepoName,
                    srcFullPath = it.fullPath,
                    destProjectId = dstProjectId,
                    destRepoName = dstRepoName,
                    destFullPath = it.fullPath,
                    overwrite = true,
                    operator = operator
                )
            )
        }
    }

    protected fun getAndCheckRepository(
        srcProjectId: String,
        srcRepoName: String,
        dstProjectId: String,
        dstRepoName: String
    ): Pair<RepositoryDetail, RepositoryDetail> {
        // 禁止移动/复制到同一仓库
        Preconditions.checkArgument(
            !(srcProjectId == dstProjectId && srcRepoName == dstRepoName), "dstProjectId/dstRepoName"
        )
        val srcRepo = repositoryClient.getRepoDetail(srcProjectId, srcRepoName).data
            ?: throw RepoNotFoundException("$srcProjectId/$srcRepoName")
        val dstRepo = repositoryClient.getRepoDetail(dstProjectId, dstRepoName).data
            ?: throw RepoNotFoundException("$dstProjectId/$dstRepoName")
        // 限制本地仓库类型
        Preconditions.checkArgument(
            srcRepo.type == dstRepo.type &&
                    srcRepo.category == RepositoryCategory.LOCAL &&
                    dstRepo.category == RepositoryCategory.LOCAL,
            "srcRepo/dstRepo"
        )
        return Pair(srcRepo, dstRepo)
    }

    protected open fun getAndCheckSrcVersion(
        projectId: String,
        repoName: String,
        packageKey: String,
        version: String,
        move: Boolean
    ): PackageVersion {
        val srcVersion = packageClient.findVersionByName(projectId, repoName, packageKey, version).data
            ?: throw VersionNotFoundException("$packageKey/$version")
        checkSrcVersion(projectId, srcVersion, packageKey, move)
        return srcVersion
    }

    protected fun checkDst(
        dstRepo: RepositoryDetail,
        packageKey: String,
        version: String,
        overwrite: Boolean
    ) {
        packageClient.findVersionByName(dstRepo.projectId, dstRepo.name, packageKey, version).data?.let { versionInfo ->
            val conflict = !overwrite || dstRepo.coverStrategy == CoverStrategy.UNCOVER ||
                    versionInfo.packageMetadata.any { it.key == LOCK_STATUS && it.value == true }
            if (conflict) throw VersionConflictException(PackageKeys.resolveName(packageKey), version)
        }
    }

    private fun checkSrcVersion(
        projectId: String,
        srcVersion: PackageVersion,
        packageKey: String,
        move: Boolean
    ) {
        val version = srcVersion.name
        if (
            srcVersion.packageMetadata.any { it.key == FORBID_STATUS && it.value == true } ||
            packageAccessRuleClient.checkPackageAccessRule(projectId, packageKey, version).data != true
        ) throw ErrorCodeException(ArtifactMessageCode.ARTIFACT_FORBIDDEN, "$packageKey/$version", HttpStatus.FORBIDDEN)

        if (move && srcVersion.packageMetadata.any { it.key == LOCK_STATUS && it.value == true })
            throw ErrorCodeException(ArtifactMessageCode.PACKAGE_LOCK, "$packageKey/$version")
    }

    companion object {
        private val logger = LoggerFactory.getLogger(ArtifactExtService::class.java)
    }
}
