package com.tencent.bkrepo.maven.util

import com.tencent.bkrepo.common.api.constant.StringPool
import com.tencent.bkrepo.maven.pojo.MavenDependency
import com.tencent.bkrepo.maven.pojo.MavenPlugin
import org.apache.maven.model.Dependency
import org.apache.maven.model.Model
import org.apache.maven.model.Plugin

object DependencyUtils {
    // 属性分隔符
    private const val SEPARATOR = StringPool.COLON

    // 属性为空时的占位符
    private const val PLACEHOLDER = StringPool.POUND

    fun parseDependency(dependency: Dependency, model: Model): MavenDependency {
        val versionStr = dependency.version
        val version: String? = if (versionStr != null && isProperty(versionStr)) {
            model.properties.getProperty(extractProperty(versionStr))
        } else {
            versionStr
        }
        return MavenDependency(
            groupId = dependency.groupId,
            artifactId = dependency.artifactId,
            version = version,
            type = dependency.type,
            scope = dependency.scope,
            classifier = dependency.classifier,
            optional = dependency.optional.toBoolean()
        )
    }

    fun parsePlugin(plugin: Plugin) = MavenPlugin(
        groupId = plugin.groupId,
        artifactId = plugin.artifactId,
        version = plugin.version
    )

    /**
     * [MavenDependency] 生成可检索字符串
     */
    fun MavenDependency.toSearchString() = StringBuilder()
        .append("dependency").append(SEPARATOR)
        .append(groupId).append(SEPARATOR)
        .append(artifactId).append(SEPARATOR)
        .append(version).append(SEPARATOR)
        .append(type).append(SEPARATOR)
        .append(classifier ?: PLACEHOLDER).append(SEPARATOR)
        .append(scope ?: PLACEHOLDER).append(SEPARATOR)
        .append(optional ?: PLACEHOLDER)
        .toString()

    /**
     * [MavenDependency] 生成可检索字符串
     * return dependency:[groupId]:[artifactId]:[version]:[type]:[classifier]
     */
    fun MavenDependency.toReverseSearchString() = StringBuilder()
        .append("dependency").append(SEPARATOR)
        .append(groupId).append(SEPARATOR)
        .append(artifactId).append(SEPARATOR)
        .append(version).append(SEPARATOR)
        .append(type).append(SEPARATOR)
        .append(classifier ?: PLACEHOLDER)
        .toString()

    fun MavenPlugin.toSearchString() = StringBuilder()
        .append("plugin").append(SEPARATOR)
        .append(groupId).append(SEPARATOR)
        .append(artifactId).append(SEPARATOR)
        .append(version ?: PLACEHOLDER)
        .toString()

    /**
     * 由可检索字符串转为[MavenDependency]
     */
    fun toMavenDependency(str: String): MavenDependency {
        val split = str.split(SEPARATOR)
        return MavenDependency(
            groupId = split[1],
            artifactId = split[2],
            version = split[3],
            type = split[4],
            classifier = transPlaceHolder(split[5]),
            scope = transPlaceHolder(split[6]),
            optional = transPlaceHolder(split[7]).toBoolean()
        )
    }

    fun toMavenPlugin(str: String): MavenPlugin {
        val split = str.split(SEPARATOR)
        return MavenPlugin(
            groupId = split[1],
            artifactId = split[2],
            version = transPlaceHolder(split[3]),
        )
    }

    private fun isProperty(str: String) = str.startsWith("\${") && str.endsWith("}")

    private fun extractProperty(str: String) = str.removePrefix("\${").removeSuffix("}")

    private fun transPlaceHolder(str: String?) = if (str == PLACEHOLDER) null else str
}
