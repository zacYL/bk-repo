package com.tencent.bkrepo.maven.util

import com.tencent.bkrepo.maven.constants.SNAPSHOT_SUFFIX
import org.apache.maven.artifact.repository.metadata.Metadata
import org.apache.maven.artifact.repository.metadata.Versioning
import org.apache.maven.model.Model
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

object MavenMetadataUtils {

    private val formatter: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyyMMddHHmmss")
    private const val elementVersion = "0okm(IJN"

    fun Metadata.reRender(): Metadata {
        this.versioning.latest?.let {
            this.versioning.latest = this.versioning.versions.last()
        }
        this.versioning.release?.let {
            this.versioning.release = this.versioning.versions.apply {
                this.add(0, elementVersion)
            }.last { version ->
                !version.endsWith(SNAPSHOT_SUFFIX)
            }
            if (this.versioning.release == elementVersion) {
                this.versioning.release = null
            }
            this.versioning.versions.remove(elementVersion)
        }
        this.versioning.lastUpdated = LocalDateTime.now(ZoneId.of("UTC")).format(formatter)
        return this
    }

    fun Metadata.initByModel(model: Model): Metadata {
        return Metadata().apply {
            this.groupId = model.groupId
            this.artifactId = model.artifactId
            this.version = model.version
            this.versioning = Versioning().apply {
                this.latest = model.version
                if (!model.version.endsWith(SNAPSHOT_SUFFIX)) {
                    this.release = model.version
                }
                this.versions = listOf(model.version)
                this.lastUpdated = LocalDateTime.now(ZoneId.of("UTC")).format(formatter)
            }
        }
    }

    fun initMetadataByGav(groupId: String, artifactId: String, version: String): Metadata {
        return Metadata().apply {
            this.groupId = groupId
            this.artifactId = artifactId
            this.versioning = Versioning().apply {
                this.latest = version
                this.release = version
                this.versions = listOf(version)
                this.lastUpdated = LocalDateTime.now(ZoneId.of("UTC")).format(formatter)
            }
        }
    }
}
