package com.tencent.bkrepo.common.metadata.service.packages

interface PackageRepairService {

    /**
     * 修复npm历史版本数据
     */
    fun repairHistoryVersion()

    /**
     * 修正包的版本数
     */
    fun repairVersionCount()

    /**
     * 补充分发来源的docker版本信息manifestPath字段
     */
    fun repairDockerManifestPath()

    /**
     * 修正npm artifactPath
     */
    fun repairNpmArtifactPath(): Map<String, Long>

    /**
     * 修正oci manifestPath
     */
    fun repairOciManifestPath(): Map<String, Long>
}
