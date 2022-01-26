package com.tencent.bkrepo.scanner.service.impl

import com.tencent.bkrepo.scanner.dao.DependencyDao
import com.tencent.bkrepo.scanner.model.TDependency
import com.tencent.bkrepo.scanner.service.DependencyService
import org.springframework.stereotype.Service

@Service
class DependencyServiceImpl(
    private val dependencyDao: DependencyDao
) : DependencyService {
    override fun insert(tDependency: TDependency) {
        with(tDependency) {
            find(projectId, repoName, packageKey, version)?.let {
                return
            }
            dependencyDao.insert(this)
        }
    }

    override fun find(
        projectId: String,
        repoName: String,
        packageKey: String,
        version: String
    ): TDependency? {
        return dependencyDao.find(projectId, repoName, packageKey, version)
    }
}
