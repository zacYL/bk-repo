package com.tencent.bkrepo.conan.service

import com.tencent.bkrepo.conan.pojo.artifact.ConanArtifactInfo
import com.tencent.bkrepo.conan.pojo.user.PackageVersionInfo

interface ConanWebService {

    fun artifactDetail(conanArtifactInfo: ConanArtifactInfo, packageKey: String, version: String): PackageVersionInfo?

}