/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
 *
 * Copyright (C) 2020 THL A29 Limited, a Tencent company.  All rights reserved.
 *
 * BK-CI 蓝鲸持续集成平台 is licensed under the MIT license.
 *
 * A copy of the MIT License is included in this file.
 *
 *
 * Terms of the MIT License:
 * ---------------------------------------------------
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package com.tencent.bkrepo.npm.service.impl

import com.tencent.bkrepo.common.api.util.JsonUtils
import com.tencent.bkrepo.common.api.util.readJsonString
import com.tencent.bkrepo.common.artifact.manager.StorageManager
import com.tencent.bkrepo.common.artifact.pojo.RepositoryCategory
import com.tencent.bkrepo.common.artifact.repository.context.ArtifactContext
import com.tencent.bkrepo.common.artifact.repository.context.ArtifactContextHolder
import com.tencent.bkrepo.common.artifact.repository.context.ArtifactQueryContext
import com.tencent.bkrepo.common.artifact.repository.context.ArtifactUploadContext
import com.tencent.bkrepo.common.artifact.repository.core.ArtifactService
import com.tencent.bkrepo.common.artifact.resolve.file.ArtifactFileFactory
import com.tencent.bkrepo.common.service.util.HeaderUtils
import com.tencent.bkrepo.npm.artifact.NpmArtifactInfo
import com.tencent.bkrepo.npm.constants.MODIFIED
import com.tencent.bkrepo.npm.constants.NPM_FILE_FULL_PATH
import com.tencent.bkrepo.npm.constants.NPM_PKG_METADATA_FULL_PATH
import com.tencent.bkrepo.npm.constants.NPM_TGZ_TARBALL_PREFIX
import com.tencent.bkrepo.npm.constants.PACKAGE_METADATA
import com.tencent.bkrepo.npm.constants.REQUEST_URI
import com.tencent.bkrepo.npm.exception.NpmArtifactNotFoundException
import com.tencent.bkrepo.npm.exception.NpmRepoNotFoundException
import com.tencent.bkrepo.npm.model.metadata.NpmPackageMetaData
import com.tencent.bkrepo.npm.model.metadata.NpmVersionMetadata
import com.tencent.bkrepo.npm.properties.NpmProperties
import com.tencent.bkrepo.npm.utils.NpmUtils
import com.tencent.bkrepo.npm.utils.TimeUtil
import com.tencent.bkrepo.repository.api.NodeClient
import com.tencent.bkrepo.repository.api.PackageClient
import com.tencent.bkrepo.repository.api.RepositoryClient
import com.tencent.bkrepo.repository.pojo.node.NodeDetail
import com.tencent.bkrepo.repository.pojo.node.NodeListOption
import com.tencent.bkrepo.repository.pojo.repo.RepositoryDetail
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import java.io.InputStream

// LateinitUsage: 抽象类中使用构造器注入会造成不便
@Suppress("LateinitUsage")
open class AbstractNpmService : ArtifactService() {

	@Autowired
	lateinit var nodeClient: NodeClient

	@Autowired
	lateinit var repositoryClient: RepositoryClient

	@Autowired
	lateinit var packageClient: PackageClient

	@Autowired
	lateinit var npmProperties: NpmProperties

	@Autowired
	lateinit var storageManager: StorageManager

	/**
	 * 查询仓库是否存在
	 */
	fun checkRepositoryExist(projectId: String, repoName: String): RepositoryDetail {
		return repositoryClient.getRepoDetail(projectId, repoName, "NPM").data ?: run {
			logger.error("check repository [$repoName] in projectId [$projectId] failed!")
			throw NpmRepoNotFoundException("repository [$repoName] in projectId [$projectId] not existed.")
		}
	}

	/**
	 * check package exists
	 */
	fun packageExist(projectId: String, repoName: String, key: String): Boolean {
		return packageClient.findPackageByKey(projectId, repoName, key).data?.let { true } ?: false
	}

	/**
	 * check package version exists
	 */
	fun packageVersionExist(projectId: String, repoName: String, key: String, version: String): Boolean {
		return packageClient.findVersionByName(projectId, repoName, key, version).data?.let { true } ?: false
	}

	/**
	 * check package history version exists
	 */
	fun packageHistoryVersionExist(projectId: String, repoName: String, key: String, version: String): Boolean {
		val packageSummary = packageClient.findPackageByKey(projectId, repoName, key).data ?: return false
		return packageSummary.historyVersion.contains(version)
	}

	/**
	 * query package metadata
	 */
	fun queryPackageInfo(
		artifactInfo: NpmArtifactInfo,
		name: String,
		showCustomTarball: Boolean = true
	): NpmPackageMetaData {
		val packageFullPath = NpmUtils.getPackageMetadataPath(name)
		val repoDetail = repositoryClient.getRepoDetail(artifactInfo.projectId, artifactInfo.repoName).data!!
		val context = ArtifactQueryContext(repoDetail, artifactInfo)
		context.putAttribute(NPM_FILE_FULL_PATH, packageFullPath)
		context.putAttribute(REQUEST_URI, name)
		val inputStream = repository.query(context) as? InputStream
				?: throw NpmArtifactNotFoundException("document not found")
		val packageMetaData = inputStream.use { JsonUtils.objectMapper.readValue(it, NpmPackageMetaData::class.java) }
		if (showCustomTarball && !showDefaultTarball()) {
			val versionsMap = packageMetaData.versions.map

			// 对于下载前的查询包信息请求，检查包数据是否完整，避免package.json缺失内容导致无法下载的问题
			val versions = versionsMap.keys
			val repoCategory = ArtifactContextHolder.getRepoDetail()?.category
			if (repoCategory != RepositoryCategory.VIRTUAL) {
				checkAndCompletePackageMetadata(
					artifactInfo, packageMetaData, versions, repoCategory != RepositoryCategory.REMOTE
				)
			}

			val iterator = versionsMap.entries.iterator()
			while (iterator.hasNext()) {
				val entry = iterator.next()
				modifyVersionMetadataTarball(artifactInfo, name, entry.value)
			}
		}
		return packageMetaData
	}

	private fun showDefaultTarball(): Boolean {
		val domain = npmProperties.domain
		val tarballPrefix = npmProperties.tarball.prefix
		val npmPrefixHeader = HeaderUtils.getHeader(NPM_TGZ_TARBALL_PREFIX).orEmpty()
		return npmPrefixHeader.isEmpty() && tarballPrefix.isEmpty() && domain.isEmpty()
	}

	protected fun modifyVersionMetadataTarball(
		artifactInfo: NpmArtifactInfo,
		name: String,
		versionMetadata: NpmVersionMetadata
	) {
		val oldTarball = versionMetadata.dist?.tarball!!
		versionMetadata.dist?.tarball =
			NpmUtils.buildPackageTgzTarball(
				oldTarball, npmProperties.domain, npmProperties.tarball.prefix, name, artifactInfo
			)
	}

	private fun checkAndCompletePackageMetadata(
		artifactInfo: NpmArtifactInfo,
		packageMetaData: NpmPackageMetaData,
		versionsInPackageMetadata: Set<String>,
		upload: Boolean
	) {
		val name = packageMetaData.name.orEmpty()
		val existVersions = queryExistVersion(artifactInfo.projectId, artifactInfo.repoName, name)
		val missingVersions = (existVersions - versionsInPackageMetadata).takeUnless { it.isEmpty() } ?: return

		missingVersions.forEach {
			val versionMetaData = queryVersionMetadata(artifactInfo, name, it) ?: return@forEach
			modifyPackageMetadata(packageMetaData, versionMetaData, it)
			logger.info("complete package.json of [$name]: add metadata of version [$it] when query package info")
		}
		if (upload) {
			uploadPackageMetadata(packageMetaData)
		}
	}

	protected fun modifyPackageMetadata(
		packageMetadata: NpmPackageMetaData,
		versionMetadata: NpmVersionMetadata,
		version: String = versionMetadata.version.orEmpty()
	) {
		val time = TimeUtil.getGMTTime()
		packageMetadata.versions.map[version] = versionMetadata
		packageMetadata.time.add(MODIFIED, time)
		packageMetadata.time.add(version, time)
	}

	protected fun uploadPackageMetadata(packageInfo: NpmPackageMetaData) {
		val name = packageInfo.name.orEmpty()
		val artifactFile = JsonUtils.objectMapper.writeValueAsBytes(packageInfo).inputStream()
			.use { ArtifactFileFactory.build(it) }
		val context = ArtifactUploadContext(artifactFile)
		val fullPath = String.format(NPM_PKG_METADATA_FULL_PATH, name)
		context.putAttribute(NPM_FILE_FULL_PATH, fullPath)
		repository.upload(context)
	}

	protected fun loadPackageMetadata(context: ArtifactContext): NpmPackageMetaData? {
		with(context) {
			return getAttribute<NpmPackageMetaData>(PACKAGE_METADATA) ?: run {
				val npmArtifactInfo = artifactInfo as NpmArtifactInfo
				val fullPath = NpmUtils.getPackageMetadataPath(npmArtifactInfo.packageName)
				val pkgMetadataNode = nodeClient.getNodeDetail(projectId, repoName, fullPath).data
				storageManager.loadArtifactInputStream(pkgMetadataNode, storageCredentials)?.use {
					it.readJsonString<NpmPackageMetaData>()
				}?.also { putAttribute(PACKAGE_METADATA, it) }
			}
		}
	}

	protected fun queryVersionMetadata(
		artifactInfo: NpmArtifactInfo,
		name: String,
		version: String
	): NpmVersionMetadata? {
		val versionMetadataPath = NpmUtils.getVersionPackageMetadataPath(name, version)
		val node = nodeClient.getNodeDetail(
			artifactInfo.projectId,
			artifactInfo.repoName,
			versionMetadataPath
		).data
		return storageManager.loadArtifactInputStream(node, null)?.use {
			JsonUtils.objectMapper.readValue(it, NpmVersionMetadata::class.java)
		}
	}

	private fun queryExistVersion(projectId: String, repoName: String, packageName: String): List<String> {
		val metadataPath = NpmUtils.getPackageMetadataPath(packageName).removeSuffix("/package.json")
		val versionMetadataNodes = queryNodes(projectId, repoName, metadataPath)
		return versionMetadataNodes.filterNot { it.name == "package.json" }
			.map { NpmUtils.analyseVersionFromVersionMetadataName(it.name, packageName) }
	}

	private fun queryNodes(projectId: String, repoName: String, fullPath: String): List<NodeDetail> {
		val nodes = mutableListOf<NodeDetail>()
		val option = NodeListOption(
			pageNumber = 1,
			pageSize = 1000,
			includeFolder = false
		)
		while (true) {
			val records = nodeClient.listNodePage(projectId, repoName, fullPath, option).data?.records
				.takeUnless { it.isNullOrEmpty() } ?: return nodes
			option.pageNumber ++
			nodes.addAll(records.map { NodeDetail(it) })
		}
	}

	companion object {
		val logger: Logger = LoggerFactory.getLogger(AbstractNpmService::class.java)
	}
}
