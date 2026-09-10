package com.tencent.bkrepo.oci.controller.user

import com.tencent.bkrepo.auth.pojo.enums.PermissionAction
import com.tencent.bkrepo.auth.pojo.enums.ResourceType
import com.tencent.bkrepo.common.security.permission.Permission
import com.tencent.bkrepo.oci.constant.ARTIFACT_TYPE
import com.tencent.bkrepo.oci.constant.DOCKER_API_VERSION
import com.tencent.bkrepo.oci.constant.DOCKER_HEADER_API_VERSION
import com.tencent.bkrepo.oci.constant.IMAGE_INDEX_MEDIA_TYPE
import com.tencent.bkrepo.oci.constant.OCI_FILTERS_APPLIED
import com.tencent.bkrepo.oci.model.ReferrersIndex
import com.tencent.bkrepo.oci.pojo.artifact.OciArtifactInfo.Companion.REFERRERS_URL
import com.tencent.bkrepo.oci.pojo.artifact.OciReferrersArtifactInfo
import com.tencent.bkrepo.oci.service.OciOperationService
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@Suppress("MVCPathVariableInspection")
class OciReferrersController(
    private val ociOperationService: OciOperationService
) {

    @Permission(type = ResourceType.REPO, action = PermissionAction.READ)
    @GetMapping(REFERRERS_URL)
    fun listReferrers(
        artifactInfo: OciReferrersArtifactInfo,
        @RequestParam(ARTIFACT_TYPE) artifactType: String?
    ): ResponseEntity<ReferrersIndex> {
        val result = ociOperationService.listReferrers(artifactInfo, artifactType)
        val headers = HttpHeaders()
        headers.set(DOCKER_HEADER_API_VERSION, DOCKER_API_VERSION)
        headers.set(HttpHeaders.CONTENT_TYPE, IMAGE_INDEX_MEDIA_TYPE)
        if (!artifactType.isNullOrBlank()) {
            headers.set(OCI_FILTERS_APPLIED, ARTIFACT_TYPE)
        }
        return ResponseEntity(result, headers, HttpStatus.OK)
    }
}
