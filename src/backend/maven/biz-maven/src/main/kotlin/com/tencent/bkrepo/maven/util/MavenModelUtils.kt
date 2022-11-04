package com.tencent.bkrepo.maven.util

import org.apache.maven.model.Model

/**
 * extend org.apache.maven.model.Model to add some useful methods
 */
object MavenModelUtils {

    fun Model.toArtifact(classifier: String? = null) =
            "$artifactId-$version${if (classifier.isNullOrBlank()) "" else "-$classifier"}.$packaging"

    fun Model.toPom(classifier: String? = null) =
            "$artifactId-$version${if (classifier.isNullOrBlank()) "" else "-$classifier"}.pom"

    fun Model.toArtifactUri(classifier: String? = null): String {
        val groupId = this.groupId.replace(".", "/").trim('/')
        val artifactId = this.artifactId.trim('/')
        val version = this.version.trim('/')
        return "$groupId/$artifactId/$version/${toArtifact(classifier)}"
    }

    fun Model.toPomUri(classifier: String? = null): String {
        val groupId = this.groupId.replace(".", "/").trim('/')
        val artifactId = this.artifactId.trim('/')
        val version = this.version.trim('/')
        return "$groupId/$artifactId/$version/${toPom(classifier)}"
    }

    fun Model.toMetadataUri(name: String = "maven-metadata.xml"): String {
        val groupId = this.groupId.replace(".", "/").trim('/')
        val artifactId = this.artifactId.trim('/')
        return "$groupId/$artifactId/$name"
    }
}
