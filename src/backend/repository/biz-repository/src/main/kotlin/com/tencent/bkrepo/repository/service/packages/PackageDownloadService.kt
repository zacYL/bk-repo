package com.tencent.bkrepo.repository.service.packages

import com.tencent.bkrepo.common.query.model.QueryModel
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody

interface PackageDownloadService {

    /**
     * 下载包版本
     *
     * @param projectId 项目id
     * @param repoName 项目id
     * @param packageKey 包唯一标识
     * @param versionName 版本名称
     */
    fun downloadVersion(
        projectId: String,
        repoName: String,
        packageKey: String,
        versionName: String,
        realIpAddress: String? = null
    )

    fun exportPackage(queryModel: QueryModel): StreamingResponseBody
}
