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

package com.tencent.bkrepo.npm.controller

import com.tencent.bk.audit.annotations.ActionAuditRecord
import com.tencent.bk.audit.annotations.AuditAttribute
import com.tencent.bk.audit.annotations.AuditEntry
import com.tencent.bk.audit.annotations.AuditInstanceRecord
import com.tencent.bkrepo.auth.pojo.enums.PermissionAction
import com.tencent.bkrepo.auth.pojo.enums.ResourceType
import com.tencent.bkrepo.common.api.constant.MediaTypes
import com.tencent.bkrepo.common.api.util.readJsonString
import com.tencent.bkrepo.common.artifact.api.ArtifactFileMap
import com.tencent.bkrepo.common.artifact.api.ArtifactPathVariable
import com.tencent.bkrepo.common.artifact.audit.ActionAuditContent
import com.tencent.bkrepo.common.artifact.audit.NODE_DOWNLOAD_ACTION
import com.tencent.bkrepo.common.artifact.audit.NODE_RESOURCE
import com.tencent.bkrepo.common.security.permission.Permission
import com.tencent.bkrepo.common.service.util.HttpContextHolder
import com.tencent.bkrepo.npm.artifact.NpmArtifactInfo
import com.tencent.bkrepo.npm.artifact.NpmTarballArtifactInfo
import com.tencent.bkrepo.npm.model.metadata.NpmPackageMetaData
import com.tencent.bkrepo.npm.model.metadata.NpmVersionMetadata
import com.tencent.bkrepo.npm.pojo.NpmDeleteResponse
import com.tencent.bkrepo.npm.pojo.NpmSearchResponse
import com.tencent.bkrepo.npm.pojo.NpmSuccessResponse
import com.tencent.bkrepo.npm.pojo.OhpmResponse
import com.tencent.bkrepo.npm.pojo.metadata.MetadataSearchRequest
import com.tencent.bkrepo.npm.pojo.metadata.disttags.DistTags
import com.tencent.bkrepo.npm.pojo.user.OhpmUnpublishRequest
import com.tencent.bkrepo.npm.service.NpmClientService
import com.tencent.bkrepo.npm.service.impl.NpmWebService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestAttribute
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestMethod
import org.springframework.web.bind.annotation.RestController

/**
 * npm 客户端操作指令
 */
@Suppress("MVCPathVariableInspection")
@RequestMapping("/{projectId}/{repoName}")
@RestController
class NpmClientController(
    private val npmClientService: NpmClientService,
    private val npmWebService: NpmWebService,
) {

    @GetMapping("/-/ping")
    fun ping(): OhpmResponse {
        return OhpmResponse.success()
    }

    /**
     * npm service info
     */
    @GetMapping
    @Permission(ResourceType.REPO, PermissionAction.READ)
    fun repoInfo(): ResponseEntity<Void> {
        return ResponseEntity.ok().build<Void>()
    }

    /**
     * npm publish or update package
     */
    @PutMapping("/{name}")
    @Permission(ResourceType.REPO, PermissionAction.WRITE)
    fun publishOrUpdatePackage(
        @RequestAttribute userId: String,
        @ArtifactPathVariable artifactInfo: NpmArtifactInfo,
        @PathVariable name: String
    ): NpmSuccessResponse {
        return npmClientService.publishOrUpdatePackage(userId, artifactInfo, name)
    }

    @PutMapping("/@{scope}/{name}")
    @Permission(ResourceType.REPO, PermissionAction.WRITE)
    fun publishOrUpdatePackage(
        @RequestAttribute userId: String,
        @ArtifactPathVariable artifactInfo: NpmArtifactInfo,
        @PathVariable scope: String,
        @PathVariable name: String
    ): NpmSuccessResponse {
        val pkgName = String.format("@%s/%s", scope, name)
        return npmClientService.publishOrUpdatePackage(userId, artifactInfo, pkgName)
    }

    @PostMapping("/stream/{name}", "/stream/@{scope}/{name}")
    @Permission(ResourceType.REPO, PermissionAction.WRITE)
    fun ohpmStreamPublishOrUpdatePackage(
        @RequestAttribute userId: String,
        @ArtifactPathVariable artifactInfo: NpmArtifactInfo,
        artifactFileMap: ArtifactFileMap,
    ): OhpmResponse {
        val npmPackageMetadata = HttpContextHolder
            .getRequest()
            .getParameter("metadata")
            .readJsonString<NpmPackageMetaData>()
        return npmClientService.ohpmStreamPublishOrUpdatePackage(
            userId,
            artifactInfo,
            npmPackageMetadata,
            artifactFileMap["pkg_stream"]!!
        )
    }

    /**
     * query package.json info
     */
    @GetMapping("/{name}", "/@{scope}/{name}")
    @Permission(ResourceType.REPO, PermissionAction.READ)
    fun packageInfo(
        @ArtifactPathVariable artifactInfo: NpmArtifactInfo
    ) {
        npmClientService.packageInfo(artifactInfo)
    }

    /**
     * query package-version.json info
     */
    @GetMapping("/{name}/{version}", produces = [MediaTypes.APPLICATION_JSON])
    @Permission(ResourceType.REPO, PermissionAction.READ)
    fun packageVersion(
        @ArtifactPathVariable artifactInfo: NpmArtifactInfo,
        @PathVariable name: String,
        @PathVariable version: String
    ): NpmVersionMetadata {
        return npmClientService.packageVersionInfo(artifactInfo, name, version)
    }

    @GetMapping("/@{scope}/{name}/{version}", produces = [MediaTypes.APPLICATION_JSON])
    @Permission(ResourceType.REPO, PermissionAction.READ)
    fun packageVersion(
        @ArtifactPathVariable artifactInfo: NpmArtifactInfo,
        @PathVariable scope: String,
        @PathVariable name: String,
        @PathVariable version: String
    ): NpmVersionMetadata {
        val pkgName = String.format("@%s/%s", scope, name)
        return npmClientService.packageVersionInfo(artifactInfo, pkgName, version)
    }

    /**
     * download tgz file
     *
     * 1. 本地仓库:
     *      /name/-/name-1.0.0.tgz
     *      /@scope/name/-/@scope/name-1.0.0.tgz
     * 2. 远程仓库:
     *      /name/-/name-1.0.0.tgz
     *      /@scope/name/-/name-1.0.0.tgz
     * 3. 远程仓库(cnpmjs.org):
     *      /name/download/name-1.0.0.tgz
     *      /@scope/name/download/@scope/name-1.0.0.tgz
     */
    @AuditEntry(
        actionId = NODE_DOWNLOAD_ACTION
    )
    @ActionAuditRecord(
        actionId = NODE_DOWNLOAD_ACTION,
        instance = AuditInstanceRecord(
            resourceType = NODE_RESOURCE,
            instanceIds = "#artifactInfo?.getArtifactFullPath()",
            instanceNames = "#artifactInfo?.getArtifactFullPath()"
        ),
        attributes = [
            AuditAttribute(name = ActionAuditContent.PROJECT_CODE_TEMPLATE, value = "#artifactInfo?.projectId"),
            AuditAttribute(name = ActionAuditContent.REPO_NAME_TEMPLATE, value = "#artifactInfo?.repoName")
        ],
        scopeId = "#artifactInfo?.projectId",
        content = ActionAuditContent.NODE_DOWNLOAD_CONTENT
    )
    @GetMapping(
        "/{name}/{delimiter:-|download}/{fileName}",
        "/@{scope}/{name}/{delimiter:-}/{fileName}",
        "/@{scope}/{name}/{delimiter:-|download}/@{repeatedScope}/{fileName}"
    )
    @Permission(ResourceType.REPO, PermissionAction.READ)
    fun download(
        @ArtifactPathVariable artifactInfo: NpmTarballArtifactInfo
    ) {
        npmClientService.download(artifactInfo)
    }

    /**
     * npm search
     */
    @GetMapping("/-/v1/search")
    @Permission(ResourceType.REPO, PermissionAction.READ)
    fun search(
        @ArtifactPathVariable artifactInfo: NpmArtifactInfo,
        searchRequest: MetadataSearchRequest
    ): NpmSearchResponse {
        return npmClientService.search(artifactInfo, searchRequest)
    }

    /**
     * npm get dist-tag ls
     */
    @GetMapping(
        "/-/package/{name}/dist-tags",
        "/-/package/@{scope}/{name}/dist-tags"
    )
    @Permission(ResourceType.REPO, PermissionAction.READ)
    fun getDistTags(
        @ArtifactPathVariable artifactInfo: NpmArtifactInfo,
        @PathVariable scope: String?,
        @PathVariable name: String
    ): DistTags {
        val pkgName = if (scope.isNullOrBlank()) name else String.format("@%s/%s", scope, name)
        return npmClientService.getDistTags(artifactInfo, pkgName)
    }

    /**
     * npm dist-tag add
     */
    @RequestMapping(
        method = [RequestMethod.POST, RequestMethod.PUT],
        path = [
            "/-/package/{name}/dist-tags/{tag}",
            "/-/package/@{scope}/{name}/dist-tags/{tag}"
        ]
    )
    @Permission(ResourceType.REPO, PermissionAction.WRITE)
    fun addDistTags(
        @RequestAttribute userId: String,
        @ArtifactPathVariable artifactInfo: NpmArtifactInfo,
        @PathVariable scope: String?,
        @PathVariable name: String,
        @PathVariable tag: String
    ): NpmSuccessResponse {
        val pkgName = if (scope.isNullOrBlank()) name else String.format("@%s/%s", scope, name)
        npmClientService.addDistTags(userId, artifactInfo, pkgName, tag)
        return NpmSuccessResponse.createTagSuccess()
    }

    /**
     * npm dist-tag rm
     */
    @DeleteMapping(
        "/-/package/{name}/dist-tags/{tag}",
        "/-/package/@{scope}/{name}/dist-tags/{tag}"
    )
    @Permission(ResourceType.REPO, PermissionAction.WRITE)
    fun deleteDistTags(
        @RequestAttribute userId: String,
        @ArtifactPathVariable artifactInfo: NpmArtifactInfo,
        @PathVariable scope: String?,
        @PathVariable name: String,
        @PathVariable tag: String
    ) {
        val pkgName = if (scope.isNullOrBlank()) name else String.format("@%s/%s", scope, name)
        npmClientService.deleteDistTags(userId, artifactInfo, pkgName, tag)
    }

    /**
     * delete the version triggers the request
     */
    @PutMapping("/{name}/-rev/{rev}", "/@{scope}/{name}/-rev/{rev}")
    @Permission(ResourceType.REPO, PermissionAction.WRITE)
    fun updatePackage(
        @RequestAttribute userId: String,
        @ArtifactPathVariable artifactInfo: NpmArtifactInfo,
        @PathVariable scope: String?,
        @PathVariable name: String,
        @PathVariable rev: String
    ): NpmSuccessResponse {
        val pkgName = if (scope.isNullOrBlank()) name else String.format("@%s/%s", scope, name)
        npmClientService.updatePackage(userId, artifactInfo, pkgName)
        return NpmSuccessResponse.updatePkgSuccess()
    }

    /**
     * npm unpublish package@1.0.0
     */
    @DeleteMapping(
        "/{name}/{delimiter:-|download}/{fileName}/-rev/{rev}",
        "/@{scope}/{name}/{delimiter:-}/{fileName}/-rev/{rev}",
        "/@{scope}/{name}/{delimiter:-|download}/@{repeatedScope}/{fileName}/-rev/{rev}"
    )
    @Permission(ResourceType.REPO, PermissionAction.DELETE)
    fun deleteVersion(
        @ArtifactPathVariable artifactInfo: NpmArtifactInfo,
        @PathVariable rev: String
    ): NpmDeleteResponse {
        npmClientService.deleteVersion(artifactInfo)
        return NpmDeleteResponse(true, artifactInfo.packageName, rev)
    }

    /**
     * npm unpublish package
     */
    @DeleteMapping("/{name}/-rev/{rev}", "/@{scope}/{name}/-rev/{rev}")
    @Permission(ResourceType.REPO, PermissionAction.DELETE)
    fun deletePackage(
        @ArtifactPathVariable artifactInfo: NpmArtifactInfo,
        @PathVariable rev: String
    ): NpmDeleteResponse {
        npmClientService.deletePackage(artifactInfo)
        return NpmDeleteResponse(true, artifactInfo.packageName, rev)
    }

    /**
     * ohpm unpublish package or package version
     */
    @DeleteMapping("/{name}", "/@{scope}/{name}")
    @Permission(ResourceType.REPO, PermissionAction.DELETE)
    fun ohpmDeletePackage(
        @RequestAttribute userId: String,
        @ArtifactPathVariable artifactInfo: NpmArtifactInfo,
        @PathVariable scope: String?,
        @PathVariable name: String,
        @RequestBody unpublishRequest: OhpmUnpublishRequest,
    ) {
        if (unpublishRequest.version.isEmpty()) {
            npmClientService.deletePackage(artifactInfo)
        } else {
            // 删除json/har/hsp文件，移除package-version记录,更新package.json文件
            npmWebService.deleteVersion(userId, artifactInfo)
        }
    }
}
