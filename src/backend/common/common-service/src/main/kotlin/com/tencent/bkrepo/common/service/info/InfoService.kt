package com.tencent.bkrepo.common.service.info

import com.tencent.bkrepo.common.service.pojo.Release
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.time.LocalDateTime

@Service
class InfoService {
    @Value("\${release.version}")
    private var version: String = "undefined"

    @Value("\${release.majorVersion}")
    private var majorVersion: String = ""

    @Value("\${release.minorVersion}")
    private var minorVersion: String = ""

    @Value("\${release.fixVersion}")
    private var fixVersion: String = ""

    @Value("\${release.buildTime}")
    private var buildTime: String = LocalDateTime.now().toString()

    @Value("\${release.description}")
    private var description: String = "©2001-2022 广州嘉为科技有限公司 版权所有"

    @Value("\${release.cicd}")
    private var cicd: String = ""

    @Value("\${release.commitId}")
    private var latestCommitId: String = ""

    fun releaseInfo() = Release(
        version = version,
        majorVersion = majorVersion,
        minorVersion = minorVersion,
        fixVersion = fixVersion,
        buildTime = buildTime,
        description = description,
        cicd = cicd,
        latestCommitId = latestCommitId
    )

    fun version(): String? {
        return if (majorVersion.isBlank()) {
            null
        } else if (minorVersion.isBlank()) {
            majorVersion
        } else if (fixVersion.isBlank()) {
            "$majorVersion.$minorVersion"
        } else {
            "$majorVersion.$minorVersion.$fixVersion"
        }
    }
}
