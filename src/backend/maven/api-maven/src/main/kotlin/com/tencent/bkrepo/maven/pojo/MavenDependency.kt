package com.tencent.bkrepo.maven.pojo

/**
 * pom 中可以定义父pom, 引入的依赖的参数可以从父pom 中读取。但是去具体看单个包的时候是看不到，除非拿到整个项目工程
 */
data class MavenDependency(
    val groupId: String,
    val artifactId: String,
    val version: String?,
    val type: String,
    val classifier: String?,
    val scope: String?,
    val optional: Boolean?,
)
