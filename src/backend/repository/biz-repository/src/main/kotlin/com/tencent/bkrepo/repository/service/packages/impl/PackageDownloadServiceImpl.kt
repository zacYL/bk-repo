package com.tencent.bkrepo.repository.service.packages.impl

import com.alibaba.excel.EasyExcel
import com.alibaba.excel.annotation.ExcelProperty
import com.tencent.bkrepo.common.api.exception.ErrorCodeException
import com.tencent.bkrepo.common.api.message.CommonMessageCode
import com.tencent.bkrepo.common.artifact.api.DefaultArtifactInfo
import com.tencent.bkrepo.common.artifact.constant.ARTIFACT_INFO_KEY
import com.tencent.bkrepo.common.artifact.exception.ArtifactDownloadForbiddenException
import com.tencent.bkrepo.common.artifact.message.ArtifactMessageCode
import com.tencent.bkrepo.common.artifact.repository.context.ArtifactContextHolder
import com.tencent.bkrepo.common.artifact.repository.context.ArtifactDownloadContext
import com.tencent.bkrepo.common.metadata.dao.packages.PackageDao
import com.tencent.bkrepo.common.metadata.dao.packages.PackageVersionDao
import com.tencent.bkrepo.common.metadata.model.TPackage
import com.tencent.bkrepo.common.metadata.model.TPackageVersion
import com.tencent.bkrepo.common.metadata.search.packages.PackageQueryContext
import com.tencent.bkrepo.common.metadata.search.packages.PackageSearchInterpreter
import com.tencent.bkrepo.common.metadata.util.MetadataUtils
import com.tencent.bkrepo.common.metadata.util.PackageEventFactory
import com.tencent.bkrepo.common.query.model.QueryModel
import com.tencent.bkrepo.common.security.util.SecurityUtils
import com.tencent.bkrepo.common.service.util.HttpContextHolder
import com.tencent.bkrepo.common.service.util.SpringContextUtils.Companion.publishEvent
import com.tencent.bkrepo.common.metadata.cpack.service.PackageAccessRuleService
import com.tencent.bkrepo.repository.pojo.packages.PackageVersion
import com.tencent.bkrepo.repository.service.packages.PackageDownloadService
import org.apache.commons.lang3.reflect.FieldUtils
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.data.util.CastUtils
import org.springframework.stereotype.Service
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody

@Service
class PackageDownloadServiceImpl(
    private val packageDao: PackageDao,
    private val packageVersionDao: PackageVersionDao,
    private val packageSearchInterpreter: PackageSearchInterpreter,
    private val packageAccessRuleService: PackageAccessRuleService,
) : PackageDownloadService {

    override fun downloadVersion(
        projectId: String,
        repoName: String,
        packageKey: String,
        versionName: String,
        realIpAddress: String?
    ) {
        val tPackage = findPackageExcludeHistoryVersion(projectId, repoName, packageKey)
        val tPackageVersion = checkPackageVersion(tPackage.id!!, versionName)
        if (tPackageVersion.artifactPath.isNullOrBlank()) {
            throw ErrorCodeException(CommonMessageCode.METHOD_NOT_ALLOWED, "artifactPath is null")
        }
        val artifactInfo = DefaultArtifactInfo(projectId, repoName, tPackageVersion.artifactPath!!)
        val context = ArtifactDownloadContext(artifact = artifactInfo, useDisposition = true)
        // 拦截package下载
        val packageVersion = convert(tPackageVersion)!!
        context.getPackageInterceptors().forEach { it.intercept(projectId, packageVersion) }
        if (!packageAccessRuleService.checkPackageAccessRule(projectId, packageKey, versionName)) {
            throw ArtifactDownloadForbiddenException(projectId)
        }
        // context 复制时会从request map中获取对应的artifactInfo， 而artifactInfo设置到map中是在接口url解析时
        HttpContextHolder.getRequestOrNull()?.setAttribute(ARTIFACT_INFO_KEY, artifactInfo)
        ArtifactContextHolder.getRepository().download(context)
        if (HttpContextHolder.getRequest().method.equals("get", ignoreCase = true)) {
            publishEvent(
                PackageEventFactory.buildDownloadEvent(
                    projectId = projectId,
                    repoName = repoName,
                    packageType = tPackage.type,
                    packageKey = packageKey,
                    packageName = tPackage.name,
                    versionName = versionName,
                    createdBy = SecurityUtils.getUserId(),
                    realIpAddress = realIpAddress ?: HttpContextHolder.getClientAddress()
                )
            )
        }
    }

    private val exportFields = FieldUtils
        .getFieldsListWithAnnotation(TPackage::class.java, ExcelProperty::class.java)
        .map { it.name }
    override fun exportPackage(queryModel: QueryModel) = StreamingResponseBody { outputStream ->
        val context = CastUtils.cast<PackageQueryContext>(packageSearchInterpreter.interpret(queryModel))
        val sort = queryModel.sort
            ?.let { Sort.by(Sort.Direction.fromString(it.direction.name), *it.properties.toTypedArray()) }
            ?: Sort.unsorted()
        var pageable = PageRequest.ofSize(500).withSort(sort)

        val writerSheet = EasyExcel.writerSheet(1).build()

        EasyExcel.write(outputStream, TPackage::class.java).includeColumnFieldNames(exportFields).build().use {
            while (true) {
                val data = packageDao.find(context.mongoQuery.with(pageable), TPackage::class.java)
                if (data.isEmpty()) {
                    break
                }
                it.write(data, writerSheet)
                if (data.size < pageable.pageSize) {
                    break
                }
                pageable = pageable.next()
            }
        }
    }

    /**
     * 查找包，不存在则抛异常
     */
    private fun findPackageExcludeHistoryVersion(projectId: String, repoName: String, packageKey: String): TPackage {
        return packageDao.findByKeyExcludeHistoryVersion(projectId, repoName, packageKey)
            ?: throw ErrorCodeException(ArtifactMessageCode.PACKAGE_NOT_FOUND, packageKey)
    }

    /**
     * 查找版本，不存在则抛异常
     */
    private fun checkPackageVersion(packageId: String, versionName: String): TPackageVersion {
        return packageVersionDao.findByName(packageId, versionName)
            ?: throw ErrorCodeException(ArtifactMessageCode.VERSION_NOT_FOUND, versionName)
    }

    fun convert(tPackageVersion: TPackageVersion?): PackageVersion? {
        return tPackageVersion?.let {
            PackageVersion(
                createdBy = it.createdBy,
                createdDate = it.createdDate,
                lastModifiedBy = it.lastModifiedBy,
                lastModifiedDate = it.lastModifiedDate,
                name = it.name,
                size = it.size,
                downloads = it.downloads,
                stageTag = it.stageTag,
                metadata = MetadataUtils.toMap(it.metadata),
                packageMetadata = MetadataUtils.toList(it.metadata),
                tags = it.tags.orEmpty(),
                extension = it.extension.orEmpty(),
                contentPath = it.artifactPath,
                contentPaths = it.artifactPaths ?: it.artifactPath?.let { path -> setOf(path) },
                manifestPath = it.manifestPath,
                clusterNames = it.clusterNames,
                recentlyUseDate = it.recentlyUseDate,
                ordinal = it.ordinal,
            )
        }
    }
}
