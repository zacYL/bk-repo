package com.tencent.bkrepo.ivy.util

import com.google.common.io.ByteStreams
import com.google.common.io.CharStreams
import com.tencent.bkrepo.common.artifact.path.PathUtils
import com.tencent.bkrepo.ivy.pojo.ParseIvyInfo
import org.apache.ivy.core.IvyPatternHelper
import org.apache.ivy.core.module.descriptor.Artifact
import org.apache.ivy.core.module.id.ModuleRevisionId
import org.apache.ivy.core.settings.IvySettings
import org.apache.ivy.plugins.parser.xml.XmlModuleDescriptorParser
import java.io.File
import java.io.InputStream
import java.io.InputStreamReader
import java.nio.charset.StandardCharsets

object IvyUtil {
    private const val MAX_DIGEST_CHARS_NEEDED = 128

    /**
     * 从流中导出摘要
     * */
    fun extractDigest(inputStream: InputStream): String {
        inputStream.use {
            val reader = InputStreamReader(
                ByteStreams
                    .limit(inputStream, MAX_DIGEST_CHARS_NEEDED.toLong()),
                StandardCharsets.UTF_8
            )
            return CharStreams.toString(reader)
        }
    }

    fun ivyParsePublishArtifacts(
        ivyXmlFile: File,
        artifactPattern: String,
        ivyPattern: String,
    ): ParseIvyInfo {

        // 设置 Ivy 配置
        val ivySettings = IvySettings()

        // 解析 ivy.xml 文件
        val descriptor = XmlModuleDescriptorParser.getInstance().parseDescriptor(
            ivySettings, ivyXmlFile.toURI().toURL(), false
        )

        val mrId = ModuleRevisionId.newInstance(
            descriptor.moduleRevisionId.organisation,
            descriptor.moduleRevisionId.name,
            descriptor.resolvedModuleRevisionId.branch,
            descriptor.revision,
            descriptor.resolvedModuleRevisionId.extraAttributes,
            false
        )

        // 合法ivy文件全路径
        val legalIvyFullPath = PathUtils.normalizeFullPath(
            IvyPatternHelper.substitute(ivyPattern, mrId, "ivy", "ivy", "xml")
        )

        // 获取主文件
        val masterArtifact = getMasterArtifact(descriptor.allArtifacts)
        val masterArtifactFullPath = masterArtifact?.let {
            PathUtils.normalizeFullPath(
                IvyPatternHelper.substitute(
                    artifactPattern,
                    mrId,
                    masterArtifact.name,
                    masterArtifact.type,
                    masterArtifact.ext
                )
            )
        }
        // 获取全部制品文件路径
        val artifactsFullPath = descriptor.allArtifacts.mapNotNull {
            PathUtils.normalizeFullPath(
                IvyPatternHelper.substitute(
                    artifactPattern,
                    mrId,
                    it.name,
                    it.type,
                    it.ext
                )
            )
        }
        return ParseIvyInfo(
            descriptor,
            legalIvyFullPath,
            artifactsFullPath,
            masterArtifactFullPath,
            masterArtifact,
        )
    }


    // ivy可以定义发布多个制品，需要找到主制品
    private fun getMasterArtifact(artifacts: Array<Artifact>): Artifact? {
        // 1、优先找master
        val master = artifacts.find { it.configurations.contains("master") }
        if (master != null) return master

        // 2、master没有找到，找runtime
        val runtime = artifacts.find { it.configurations.contains("master") }
        if (runtime != null) return runtime

        // 3、runtime没有找到，找default
        val default = artifacts.find { it.configurations.contains("default") }
        if (default != null) return default

        // 3、default没有找到，找type=jar
        val jar = artifacts.find { it.type == "jar" }
        if (jar != null) return jar

        // 都未找到，则返回第一个
        return artifacts.firstOrNull()
    }


}
