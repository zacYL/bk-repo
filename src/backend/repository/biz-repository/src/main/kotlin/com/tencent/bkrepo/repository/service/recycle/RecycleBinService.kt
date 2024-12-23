package com.tencent.bkrepo.repository.service.recycle

import com.tencent.bkrepo.common.artifact.api.ArtifactInfo

/**
 * 回收站服务接口
 */
interface RecycleBinService {

    fun irreversibleDelete(artifactInfo: ArtifactInfo, deletedId: Long)

    fun clean(projectId: String, repoName: String)
}
