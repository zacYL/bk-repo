package com.tencent.bkrepo.maven.util

import com.tencent.bkrepo.maven.constants.SNAPSHOT_SUFFIX
import com.tencent.bkrepo.maven.pojo.MavenVersion
import org.apache.commons.lang3.StringUtils
import org.apache.maven.model.Model

/**
 * extend org.apache.maven.model.Model to add some useful methods
 */
object MavenModelUtils {

    fun Model.toArtifact(classifier: String? = null) =
            "$artifactId-$version${if (classifier.isNullOrBlank()) "" else "-$classifier"}.$packaging"

    fun Model.toPom() = "$artifactId-$version.pom"
    private fun Model.groupUri() = groupId.replace(".", "/").trim('/')
    private fun Model.artifactUri() = artifactId.trim('/')
    private fun Model.versionUri() = version.trim('/')

    private fun Model.toSnapshotPom(mavenVersion: MavenVersion): String {
        val list = mutableListOf(
                artifactId, version.removeSuffix(SNAPSHOT_SUFFIX), mavenVersion.timestamp, mavenVersion.buildNo)
        return "${StringUtils.join(list, '-')}.pom"
    }

    fun Model.toArtifactUri(classifier: String? = null): String {
        return "${groupUri()}/${artifactUri()}/${versionUri()}/${toArtifact(classifier)}"
    }

    fun Model.toSnapshotArtifactUri(mavenVersion: MavenVersion): String {
        return "${groupUri()}/${artifactUri()}/${versionUri()}/${mavenVersion.combineToUnique()}"
    }

    fun Model.toPomUri(): String {
        return "${groupUri()}/${artifactUri()}/${versionUri()}/${toPom()}"
    }

    fun Model.toSnapshotPomUri(mavenVersion: MavenVersion): String {
        return "${groupUri()}/${artifactUri()}/${versionUri()}/${toSnapshotPom(mavenVersion)}"
    }

    fun Model.toMetadataUri(name: String = "maven-metadata.xml"): String {
        return "${groupUri()}/${artifactUri()}/$name"
    }

    fun Model.toSnapshotMetadataUri(name: String = "maven-metadata.xml"): String {
        return "${groupUri()}/${artifactUri()}/$version/$name"
    }
}
