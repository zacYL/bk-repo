package com.tencent.bkrepo.nuget.service

import com.tencent.bkrepo.common.artifact.repository.context.ArtifactContext
import com.tencent.bkrepo.repository.pojo.packages.PackageVersion

interface NugetOperationService {
    fun packageVersion(context: ArtifactContext): PackageVersion?
}
