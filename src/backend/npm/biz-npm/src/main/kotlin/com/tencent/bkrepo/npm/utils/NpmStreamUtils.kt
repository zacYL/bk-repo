package com.tencent.bkrepo.npm.utils

import com.tencent.bkrepo.common.artifact.resolve.file.ArtifactFileFactory
import com.tencent.bkrepo.common.artifact.stream.ArtifactInputStream
import com.tencent.bkrepo.common.artifact.stream.Range
import com.tencent.bkrepo.common.artifact.stream.artifactStream

object NpmStreamUtils {
    fun String.toArtifactStream(): ArtifactInputStream {
        val content = this.toByteArray()
        val size = content.size.toLong()
        return if (size > 2 * 1024 * 1024) {
            val artifactFile = ArtifactFileFactory.build(content.inputStream())
            artifactFile.getInputStream().artifactStream(Range.full(artifactFile.getSize()))
        } else content.inputStream().artifactStream(Range.full(size))
    }
}
