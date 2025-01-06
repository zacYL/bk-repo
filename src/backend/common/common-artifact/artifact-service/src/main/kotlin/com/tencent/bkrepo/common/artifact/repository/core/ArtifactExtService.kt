package com.tencent.bkrepo.common.artifact.repository.core

import com.tencent.bkrepo.auth.pojo.enums.PermissionAction
import com.tencent.bkrepo.common.api.util.Preconditions
import com.tencent.bkrepo.common.artifact.api.ArtifactInfo
import com.tencent.bkrepo.common.artifact.constant.ARTIFACT_INFO_KEY
import com.tencent.bkrepo.common.artifact.constant.RESERVED_KEY
import com.tencent.bkrepo.common.artifact.exception.NodeNotFoundException
import com.tencent.bkrepo.common.artifact.exception.VersionConflictException
import com.tencent.bkrepo.common.artifact.exception.VersionNotFoundException
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
import com.tencent.bkrepo.repository.api.PackageClient
import com.tencent.bkrepo.repository.api.RepositoryClient
import com.tencent.bkrepo.repository.constant.CoverStrategy
import com.tencent.bkrepo.repository.pojo.node.service.NodeMoveCopyRequest
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
            val srcRepo = repositoryClient.getRepoDetail(srcProjectId, srcRepoName).data!!
            val dstRepo = repositoryClient.getRepoDetail(dstProjectId, dstRepoName).data!!
            preCheck(srcRepo, dstRepo, packageKey, version, overwrite)
            val srcVersion = packageClient.findVersionByName(srcProjectId, srcRepoName, packageKey, version).data
                ?: throw VersionNotFoundException("$packageKey/$version")
            copyNodes(
                srcProjectId, srcRepoName, dstProjectId, dstRepoName, packageKey, version,
                srcVersion.manifestPath, srcVersion.contentPath
            )
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
    private fun getArtifactFullPaths(
        projectId: String,
        repoName: String,
        packageKey: String,
        version: String,
        manifestPath: String?,
        artifactPath: String?
    ): List<String> {
        return (ArtifactContextHolder.getRepository(RepositoryCategory.LOCAL) as LocalRepository)
            .getArtifactFullPaths(projectId, repoName, packageKey, version, manifestPath, artifactPath)
            .also {
                logger.info("artifactFullPaths of [$projectId/$repoName/$packageKey/$version]: [$it]")
                require(it.isNotEmpty())
                checkNodeExist(projectId, repoName, it)
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
        getArtifactFullPaths(srcProjectId, srcRepoName, packageKey, version, manifestPath, artifactPath).forEach {
            nodeClient.copyNode(
                NodeMoveCopyRequest(
                    srcProjectId = srcProjectId,
                    srcRepoName = srcRepoName,
                    srcFullPath = it,
                    destProjectId = dstProjectId,
                    destRepoName = dstRepoName,
                    destFullPath = it,
                    overwrite = true,
                    operator = operator
                )
            )
        }
        updateIndex(dstProjectId, dstRepoName, packageKey, version)
    }

    private fun preCheck(
        srcRepo: RepositoryDetail,
        dstRepo: RepositoryDetail,
        packageKey: String,
        version: String,
        overwrite: Boolean
    ) {
        Preconditions.checkArgument(
            !(srcRepo.projectId == dstRepo.projectId && srcRepo.name == dstRepo.name),
            "dstProjectId/dstRepoName"
        )
        Preconditions.checkArgument(
            srcRepo.type == dstRepo.type &&
                    srcRepo.category == RepositoryCategory.LOCAL &&
                    dstRepo.category == RepositoryCategory.LOCAL,
            "srcRepo/dstRepo"
        )
        if (
            packageClient.findVersionByName(dstRepo.projectId, dstRepo.name, packageKey, version).data != null &&
            (!overwrite || dstRepo.coverStrategy == CoverStrategy.UNCOVER)
        ) throw VersionConflictException(PackageKeys.resolveName(packageKey), version)
    }

    private fun checkNodeExist(projectId: String, repoName: String, fullPaths: List<String>) {
        val existedList = nodeClient.listExistFullPath(projectId, repoName, fullPaths).data ?: emptyList()
        if (existedList.size != fullPaths.size) {
            throw NodeNotFoundException((fullPaths - existedList.toSet()).first())
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(ArtifactExtService::class.java)
    }
}
