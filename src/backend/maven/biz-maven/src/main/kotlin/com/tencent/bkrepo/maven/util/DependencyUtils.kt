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

    // 处理dependency
    fun parseDependency(dependency: Dependency, model: Model, parentPom: Model?): MavenDependency {
        val versionStr = dependency.version
        val version: String? = if (versionStr != null) {
            versionResolver(versionStr, model)
        } else {
            if (model.dependencyManagement != null) {
                var versionTemp = handleDependenciesFromManagement(dependency, model)
                if (versionTemp == "null") {
                    versionTemp = handleDependenciesFromParent(dependency, parentPom)
                }
                versionTemp
            } else {
                handleDependenciesFromParent(dependency, parentPom)
            }
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

    // 如果是直接存在就返回，如果是占位符号的形式存在，那就寻找对应的属性
    private fun versionResolver(versionStr: String, model: Model): String? =
        if (isProperty(versionStr)) {
            model.properties.getProperty(extractProperty(versionStr))
        } else {
            versionStr
        }

    // 从dependencyManagement中查找版本信息
    private fun handleDependenciesFromManagement(
        dependency: Dependency,
        pom: Model
    ): String? {
        var version: String? = "null"

        pom.dependencyManagement.dependencies.forEach { ptDependency ->
            if (dependency.groupId == ptDependency.groupId &&
                dependency.artifactId == ptDependency.artifactId
            ) {
                // 匹配了version之后就及时返回
                if (ptDependency.version != null) {
                    version = versionResolver(ptDependency.version, pom)
                }
                return@forEach
            }
        }
        return version
    }

    // 从父pom中查找依赖版本信息
    private fun handleDependenciesFromParent(
        dependency: Dependency,
        parentPom: Model?
    ): String? {
        return if (parentPom == null) {
            "null"
        } else {
            var version: String? = "null"
            version = handleDependencyFromDependencies(parentPom, dependency)
            if (version == "null" && parentPom.dependencyManagement != null) {
                version = handleDependenciesFromManagement(dependency, parentPom)
            }
            version
        }
    }

    // 从父pom中的dependencies中进行匹配
    private fun handleDependencyFromDependencies(
        parentPom: Model,
        dependency: Dependency
    ): String? {
        var newVersion: String? = "null"
        parentPom.dependencies.forEach { ptDependency ->
            if (dependency.groupId == ptDependency.groupId &&
                dependency.artifactId == ptDependency.artifactId
            ) {

                if (ptDependency.version != null) {
                    newVersion = versionResolver(ptDependency.version, parentPom)
                }
            }
        }
        return newVersion
    }

    // 处理插件版本信息
    fun parsePlugin(plugin: Plugin, model: Model, parentPom: Model?): MavenPlugin {
        val versionStr = plugin.version
        val version: String? = if (versionStr != null) {
            versionResolver(versionStr, model)
        } else {
            if (model.build.pluginManagement != null) {
                var versionTemp = handlePluginsFromManagement(plugin, model)
                if (versionTemp == "null") {
                    versionTemp = handlePluginsFromParent(plugin, parentPom)
                }
                versionTemp
            } else {
                handlePluginsFromParent(plugin, parentPom)
            }
        }
        return MavenPlugin(
            groupId = plugin.groupId,
            artifactId = plugin.artifactId,
            version = version
        )
    }

    // 从pluginManagement中查找插件版本信息
    private fun handlePluginsFromManagement(
        plugin: Plugin,
        pom: Model
    ): String? {
        var version: String? = "null"
        pom.build.pluginManagement.plugins.forEach { ptPlugin ->
            if (plugin.groupId == ptPlugin.groupId &&
                plugin.artifactId == ptPlugin.artifactId
            ) {
                // 匹配了version之后就及时返回
                if (ptPlugin.version != null) {
                    version = versionResolver(ptPlugin.version, pom)
                }
                return@forEach
            }
        }
        return version
    }

    // 从父pom中查找插件依赖信息
    private fun handlePluginsFromParent(
        plugin: Plugin,
        parentPom: Model?
    ): String? {
        return if (parentPom == null) {
            "null"
        } else {
            var version: String? = "null"
            version = handlePluginFromPlugins(parentPom, plugin)
            if (version == "null" && parentPom.build.pluginManagement != null) {
                version = handlePluginsFromManagement(plugin, parentPom)
            }
            version
        }
    }

    // 从父pom中plugins中一个个匹配查找
    private fun handlePluginFromPlugins(
        parentPom: Model,
        plugin: Plugin
    ): String? {
        var newVersion: String? = "null"
        parentPom.build.plugins.forEach { ptPlugin ->
            if (plugin.groupId == ptPlugin.groupId &&
                plugin.artifactId == ptPlugin.artifactId
            ) {

                if (ptPlugin.version != null) {
                    newVersion = versionResolver(ptPlugin.version, parentPom)
                }
            }
        }
        return newVersion
    }

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
