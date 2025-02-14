package com.tencent.bkrepo.maven.service

import com.tencent.bkrepo.repository.pojo.node.NodeDetail
import com.tencent.bkrepo.repository.pojo.packages.PackageVersion

interface MavenOperationService {
    fun packageVersion(node: NodeDetail): PackageVersion?
}
