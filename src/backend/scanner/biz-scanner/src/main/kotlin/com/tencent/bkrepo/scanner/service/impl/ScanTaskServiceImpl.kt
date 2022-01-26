package com.tencent.bkrepo.scanner.service.impl

import com.tencent.bkrepo.common.api.util.readJsonString
import com.tencent.bkrepo.common.api.util.toJsonString
import com.tencent.bkrepo.common.checker.pojo.Dependency
import com.tencent.bkrepo.common.checker.pojo.DependencyInfo
import com.tencent.bkrepo.common.checker.pojo.Vulnerability
import com.tencent.bkrepo.common.checker.pojo.VulnerabilitySeverity
import com.tencent.bkrepo.common.checker.util.DependencyCheckerUtils
import com.tencent.bkrepo.common.storage.core.StorageProperties
import com.tencent.bkrepo.scanner.model.TDependency
import com.tencent.bkrepo.scanner.model.TVulnerability
import com.tencent.bkrepo.scanner.service.DependencyService
import com.tencent.bkrepo.scanner.service.ScanTaskService
import com.tencent.bkrepo.scanner.service.VulnerabilityService
import org.springframework.stereotype.Service
import java.util.regex.Pattern
import kotlin.random.Random

@Service
class ScanTaskServiceImpl(
    private val dependencyService: DependencyService,
    private val vulnerabilityService: VulnerabilityService,
    private val storageProperties: StorageProperties
) : ScanTaskService {
    override fun createTask(
        projectId: String,
        repoName: String,
        packageKey: String,
        version: String,
        sha256: String
    ): Boolean {
        val first = sha256.substring(0, 2)
        val second = sha256.substring(2, 4)
        val path = storageProperties.filesystem.path
        val storePath = if (!path.startsWith("/")) {
            "${System.getProperties()["user.dir"]}/$path".removeSuffix("/")
        } else {
            path.removeSuffix("/")
        }
        val filePath = "$storePath/$first/$second/$sha256"
        val scanReportMap = DependencyCheckerUtils.run(filePath)
        val report = scanReportMap["report"]
        val dependencyInfo = (report as? String)?.readJsonString<DependencyInfo>()
        dependencyInfo?.dependencies?.filter { includeBaseName(it.fileName) }?.forEach {
            val tDependency = transDependency(projectId, repoName, packageKey, version, filePath, it)
            dependencyService.insert(tDependency)
            it.vulnerabilities?.forEach { vulnerability ->
                val tVulnerability = transVulnerability(vulnerability)
                vulnerabilityService.insert(tVulnerability)
            }
        }
        return true
    }

    private fun transDependency(
        projectId: String,
        repoName: String,
        packageKey: String,
        version: String,
        artifactPath: String,
        dependency: Dependency
    ): TDependency {
        return TDependency(
            projectId = projectId,
            repoName = repoName,
            packageKey = packageKey,
            version = version,
            applyId = Random.nextInt(100000).toString(),
            description = dependency.description,
            evidenceCollected = dependency.evidenceCollected.toJsonString(),
            fileName = artifactPath,
            isVirtual = dependency.isVirtual,
            license = dependency.license,
            md5 = dependency.md5,
            packages = dependency.packages.toJsonString(),
            sha1 = dependency.sha1,
            sha256 = dependency.sha256,
            vulnerabilityIds = dependency.vulnerabilityIds?.toJsonString(),
            vulnerabilities = dependency.vulnerabilities?.map { it.name }
        )
    }

    private fun transVulnerability(vulnerability: Vulnerability): TVulnerability {
        return TVulnerability(
            source = vulnerability.source,
            name = vulnerability.name,
            cvssv2 = vulnerability.cvssv2?.toJsonString(),
            cvssv3 = vulnerability.cvssv3?.toJsonString(),
            cwes = vulnerability.cwes,
            description = vulnerability.description,
            notes = vulnerability.notes,
            references = vulnerability.references.map { it.toJsonString() },
            severity = VulnerabilitySeverity.valueOf(vulnerability.severity.toUpperCase()),
            vulnerableSoftware = vulnerability.vulnerableSoftware.map { it.toJsonString() }
        )
    }

    private fun includeBaseName(baseName: String): Boolean {
        val regex = "^[0-9a-f]{64} \\(shaded: .*\\)$"
        val matcher = Pattern.compile(regex).matcher(baseName)
        return matcher.matches()
    }
}
