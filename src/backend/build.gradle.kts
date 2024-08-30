
import java.io.ByteArrayOutputStream
import java.time.LocalDateTime

/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
 *
 * Copyright (C) 2020 THL A29 Limited, a Tencent company.  All rights reserved.
 *
 * BK-CI 蓝鲸持续集成平台 is licensed under the MIT license.
 *
 * A copy of the MIT License is included in this file.
 *
 *
 * Terms of the MIT License:
 * ---------------------------------------------------
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

plugins {
    id("com.tencent.devops.boot") version Versions.DevopsBoot
    id("com.tencent.devops.publish") version Versions.DevopsBoot apply false
}

allprojects {
    group = Release.Group
    version = Release.Version

    repositories {
        maven(url = "https://repo.spring.io/milestone")
    }

    apply(plugin = "com.tencent.devops.boot")
    apply(plugin = "jacoco")

    dependencyManagement {
        applyMavenExclusions(false)

        imports {
            mavenBom("org.springframework.cloud:spring-cloud-sleuth-otel-dependencies:${Versions.SleuthOtel}")
        }

        dependencies {
            dependency("com.github.zafarkhaja:java-semver:${Versions.JavaSemver}")
            dependency("org.apache.skywalking:apm-toolkit-logback-1.x:${Versions.SkyWalkingApmToolkit}")
            dependency("org.apache.skywalking:apm-toolkit-trace:${Versions.SkyWalkingApmToolkit}")
            dependency("net.javacrumbs.shedlock:shedlock-spring:${Versions.Shedlock}")
            dependency("net.javacrumbs.shedlock:shedlock-provider-mongo:${Versions.Shedlock}")
            dependency("com.google.code.gson:gson:${Versions.Gson}")
            dependency("org.eclipse.jgit:org.eclipse.jgit.http.server:${Versions.JGit}")
            dependency("org.eclipse.jgit:org.eclipse.jgit:${Versions.JGit}")
            dependency("org.apache.commons:commons-compress:${Versions.CommonsCompress}:")
            dependency("commons-io:commons-io:${Versions.CommonsIO}")
            dependency("com.google.guava:guava:${Versions.Guava}")
            dependency("com.google.protobuf:protobuf-java-util:${Versions.ProtobufJava}")
            dependency("com.tencent.polaris:polaris-discovery-factory:${Versions.Polaris}")
            dependency("org.apache.commons:commons-text:${Versions.CommonsText}")
            dependency("org.mockito.kotlin:mockito-kotlin:${Versions.MockitoKotlin}")
            dependency("com.h2database:h2:${Versions.H2}")
            dependency("io.mockk:mockk:${Versions.Mockk}")
            dependencySet("io.swagger:${Versions.Swagger}") {
                entry("swagger-annotations")
                entry("swagger-models")
            }
            dependency("com.tencent.bk.sdk:crypto-java-sdk:${Versions.CryptoJavaSdk}")
            dependency("org.yaml:snakeyaml:${Versions.SnakeYaml}")
            dependencySet("ch.qos.logback:${Versions.Logback}") {
                entry("logback-classic")
                entry("logback-core")
            }
            dependency("com.google.protobuf:protobuf-java:${Versions.ProtobufJava}")
            dependencySet("io.undertow:${Versions.Undertow}") {
                entry("undertow-core")
                entry("undertow-servlet")
            }
            dependency("org.springframework:spring-webmvc:${Versions.SpringWebmvc}")
            dependency("org.springframework:spring-beans:${Versions.SpringBeans}")
            dependency("org.springframework:spring-web:${Versions.SpringWeb}")
            dependencySet("org.springframework.boot:${Versions.SpringBootAutoconfigure}") {
                entry("spring-boot-autoconfigure")
                entry("spring-boot-actuator-autoconfigure")
            }
            dependency("net.minidev:json-smart:${Versions.JsonSmart}")
            dependencySet("com.fasterxml.jackson.core:${Versions.Jackson}") {
                entry("jackson-annotations")
                entry("jackson-core")
                entry("jackson-databind")
            }
            dependencySet("com.fasterxml.jackson.jaxrs:${Versions.Jackson}") {
                entry("jackson-jaxrs-base")
                entry("jackson-jaxrs-json-provider")
            }
            dependencySet("com.fasterxml.jackson.module:${Versions.Jackson}") {
                entry("jackson-module-kotlin")
                entry("jackson-module-parameter-names")
                entry("jackson-module-afterburner")
                entry("jackson-module-jaxb-annotations")
            }
            dependencySet("com.fasterxml.jackson.datatype:${Versions.Jackson}") {
                entry("jackson-datatype-jdk8")
                entry("jackson-datatype-jsr310")
            }
            dependencySet("com.fasterxml.jackson.dataformat:${Versions.Jackson}") {
                entry("jackson-dataformat-yaml")
                entry("jackson-dataformat-xml")
                entry("jackson-dataformat-cbor")
            }
            dependency("com.tongweb.springboot:tongweb-spring-boot-starter-2.x:${Versions.TongWeb}")
        }
    }

    dependencies {
        constraints {
            implementation("com.squareup.okio:okio:${Versions.Okio}")
            implementation("commons-fileupload:commons-fileupload:${Versions.CommonsFileupload}")
            implementation("com.fasterxml.woodstox:woodstox-core:${Versions.Woodstox}")
        }
    }

    ext["netty.version"] = Versions.Netty
    // 2.1.2才支持配置使用信号量隔离
    ext["spring-cloud-circuitbreaker.version"] = Versions.SpringCloudCircuitbreaker

    configurations.all {
        // io.netty:netty已替换成io.netty:netty-all
        exclude(group = "io.netty", module = "netty")
        exclude(group = "log4j", module = "log4j")
        exclude(group = "org.slf4j", module = "slf4j-log4j12")
        exclude(group = "commons-logging", module = "commons-logging")
        exclude(group = "org.springframework.data", module = "spring-data-mongodb")
        exclude(group = "org.springframework.cloud", module = "spring-cloud-function-context")
        exclude(group = "org.springframework.cloud", module = "spring-cloud-function-core")
    }

    tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
        kotlinOptions {
            freeCompilerArgs = listOf("-java-parameters")
        }
    }

    repositories {
        maven {
            isAllowInsecureProtocol = true
            setUrl("https://bkrepo.cwoa.net/maven/devops/devops-maven")
            credentials {
                username = "admin"
                password = "bkrepo"
            }
        }
    }

    tasks.withType<JacocoReport> {
        reports {
            xml.required.set(true)
            html.required.set(true)
        }
        dependsOn(tasks.getByName("test"))
    }

    fun getGitCommitId(): String {
        ByteArrayOutputStream().use { out ->
            exec {
                commandLine("git", "rev-parse", "HEAD")
                standardOutput = out
            }
            return out.toString().trim()
        }
    }

    tasks.withType<ProcessResources> {
        // 不从缓存中加载信息
        outputs.upToDateWhen { false }
        filesMatching("**/*.properties") {
            filter {
                it.replace("@release.version@", Release.Version)
            }
            filter {
                it.replace("@release.description@", "https://www.canway.net/Cpack/978.html")
            }
            filter {
                it.replace("@release.majorVersion@", System.getenv("BK_CI_MAJOR_VERSION") ?: "")
            }
            filter {
                it.replace("@release.minorVersion@", System.getenv("BK_CI_MINOR_VERSION") ?: "")
            }
            filter {
                it.replace("@release.fixVersion@", System.getenv("BK_CI_FIX_VERSION") ?: "")
            }
            filter {
                it.replace("@release.buildTime@", LocalDateTime.now().toString())
            }
            filter {
                it.replace(
                    "@release.cicd@",
                    System.getenv("BK_CI_PROJECT_NAME") +
                        "/${System.getenv("BK_CI_PIPELINE_ID")}/${System.getenv("BK_CI_BUILD_NUM")} ." +
                        " Build branch: [${System.getenv("branch") ?: ""}]"
                )
            }
            filter {
                it.replace("@release.commitId@", getGitCommitId())
            }
        }
    }

    if (isBootProject(this)) {
        tasks.named("copyToRelease") {
            dependsOn(tasks.named("bootJar"))
        }
    }

    tasks.withType<Jar> {
        duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    }
}

fun isBootProject(project: Project): Boolean {
    return project.name.startsWith("boot-") || project.findProperty("devops.boot") == "true"
}

apply(from = rootProject.file("gradle/publish-api.gradle.kts"))
apply(from = rootProject.file("gradle/publish-all.gradle.kts"))
