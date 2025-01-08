package com.tencent.bkrepo.ivy.pojo

import io.swagger.annotations.Api
import io.swagger.annotations.ApiModelProperty
import org.apache.ivy.core.module.descriptor.Artifact
import org.apache.ivy.core.module.descriptor.ModuleDescriptor

@Api("解析ivy文件信息")
data class ParseIvyInfo(
    @ApiModelProperty("ivy配置文件解析获取的对应性")
    val model: ModuleDescriptor,
    @ApiModelProperty("合法的Ivy文件全路径")
    val legalIvyFullPath: String,
    @ApiModelProperty("发布制品文件全路径（可发布多个）")
    val artifactsFullPath: List<String>,
    @ApiModelProperty("主文件全路径")
    val masterArtifactFullPath: String?,
    @ApiModelProperty("主文件制品信息")
    val masterArtifact: Artifact?,
) {
    fun isLegalPathIvyFile(projectId: String, repoName: String, requestFullPath: String): Boolean {
        val requestPath = requestFullPath.substringAfter("/$projectId/$repoName")
        return requestPath == legalIvyFullPath
    }
}
