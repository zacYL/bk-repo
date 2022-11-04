package com.tencent.bkrepo.maven.util

import org.apache.maven.model.Model

/**
 * extend org.apache.maven.model.Model to add some useful methods
 */
object MavenModelUtils {

    fun Model.toArtifactUri(filename: String): String {
        val groupId = this.groupId.replace(".", "/").trim('/')
        val artifactId = this.artifactId.trim('/')
        val version = this.version.trim('/')
        val packaging = this.packaging.trim('/')
        return "$groupId/$artifactId/$version/$filename"
    }

    fun Model.toMetadataUri(name: String = "maven-metadata.xml"): String {
        val groupId = this.groupId.replace(".", "/").trim('/')
        val artifactId = this.artifactId.trim('/')
        return "$groupId/$artifactId/$name"
    }
}
