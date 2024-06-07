package com.tencent.bkrepo.npm.service

import com.tencent.bkrepo.common.artifact.repository.context.ArtifactContext
import com.tencent.bkrepo.repository.pojo.packages.PackageVersion

interface NpmOperationService {
    fun packageVersion(context: ArtifactContext): PackageVersion?
}
