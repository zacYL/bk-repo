package com.tencent.bkrepo.scanner.service

import com.tencent.bkrepo.scanner.model.TDependency

interface DependencyService {
    fun insert(tDependency: TDependency)

    fun find(projectId: String, repoName: String, packageKey: String, version: String): TDependency?
}
