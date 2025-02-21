package utils

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream
import org.gradle.api.Project
import java.io.File
import java.nio.file.Files
import java.nio.file.Paths

object DockerUtil {
    data class Architecture(
        val platform: String,
        val cpu: String
    )

    private val architectures = arrayOf(
        Architecture("linux/amd64", "x64"),
        Architecture("linux/arm64", "arm64")
    )

    fun buildImage(slimContext: SlimContext) {
        with(slimContext) {
            prepareDockerCli()
            createBuildxIfNotExist()

            val dockerFile = File(tarFiles.dockerDir, "${serviceName}_private.Dockerfile")

            val publishScript = StringBuilder(
                """
                #!/bin/bash
                
                image_repo=${'$'}1
                # 检查 image_repo 是否为空
                if [ -z "${'$'}image_repo" ]; then
                    echo "image_repo 不能为空"
                    exit 1
                fi

                echo "image_repo: ${'$'}image_repo"
                """.trimIndent()
            ).append("\n").append("\n")

            for (arch in architectures) {
                println("开始构建$serviceName ${arch.platform}镜像")

                val tag = "$serviceName:${getBuildVersion()}-${arch.cpu}"
                val outputFile = File(tarFiles.k8sDir.absolutePath, "$serviceName/$serviceName-${arch.cpu}.tar")

                val buildResult = buildImage(
                    tag = tag,
                    buildDir = tarFiles.dockerDir.absolutePath,
                    dockerFile = dockerFile.absolutePath,
                    outputFile = outputFile.absolutePath,
                    platform = arch.platform
                )

                if (buildResult != 0) {
                    throw RuntimeException("${serviceName}镜像构建失败: exitCode=$buildResult")
                }

                println("生成$serviceName-${arch.cpu}镜像元数据文件")
                File(tarFiles.k8sDir.absolutePath, "$serviceName/$serviceName-${arch.cpu}-image-metadata").writeText(
                    """
                        name: $serviceName
                        tag: $tag
                        imageId: ${getImageIdFromTar(outputFile)}
                        md5: ${FileUtil.md5(outputFile)}
                        platform: ${arch.platform}
                    """.trimIndent()
                )

                val deployTag = "${arch.cpu}_deploy_tag"
                publishScript
                    .append(
                        """
                        echo "开始推送: $deployTag"
                        $deployTag=${'$'}image_repo/$tag
                        docker load -i ${outputFile.name}
                        docker tag $tag  ${'$'}$deployTag
                        docker push ${'$'}$deployTag
                        """.trimIndent()
                    ).append("\n")

                println("镜像${tag}构建完成")
            }

            publishScript
                .append(
                    """
                    echo "开始推送多架构镜像: $serviceName:${getBuildVersion()}"
                    docker buildx imagetools create \
                      --tag ${'$'}image_repo/$serviceName:${getBuildVersion()} \
                      ${'$'}x64_deploy_tag \
                      ${'$'}arm64_deploy_tag
                    """.trimIndent()
                ).append("\n")

            val publishScriptFile = File(tarFiles.k8sDir.absolutePath, "$serviceName/publish_image.sh")
            publishScriptFile.writeText(publishScript.toString())
            println("publish_image脚本已生成")
            if (OsUtil.isLinux()) {
                FileUtil.chmodX(publishScriptFile)
            }
        }
    }

    private fun SlimContext.prepareDockerCli() {
        val dockerHost = System.getenv("DOCKER_HOST")
        if (!dockerHost.isNullOrBlank()) {
            println("连接远程Docker服务: $dockerHost")
        }
        val dockerCheck = OsUtil.execCommand("docker", "version").exitCode
        if (dockerCheck != 0) {
            println("docker version检查失败，尝试下载docker-client")
            val repoPath = project.findPropertyOrDefault("devops_docker", "/script/slim/docker-cli/docker")
            val dockerClientFile = downloadFromCustom(repoPath)

            println("docker-client下载完毕，开始移动到/usr/bin/docker")
            FileUtil.chmodX(dockerClientFile)

            Files.move(dockerClientFile.toPath(), Paths.get("/usr/bin/docker"))
            println("docker-client准备完毕")
        }

        val dockerBuildxCheck = OsUtil.execCommand("docker", "buildx", "version").exitCode
        if (dockerBuildxCheck != 0) {
            println("docker buildx version检查失败，尝试下载docker-buildx")
            val repoPath = project.findPropertyOrDefault(
                "devops_docker_buildx",
                "/script/slim/docker-cli/docker-buildx"
            )
            val dockerBuildxFile = downloadFromCustom(repoPath)

            println("docker-buildx下载完毕，开始移动到/usr/libexec/docker/cli-plugins/")
            FileUtil.chmodX(dockerBuildxFile)
            val ciPluginDir = FileUtil.createDir("/usr/libexec/docker/cli-plugins")
            Files.move(dockerBuildxFile.toPath(), Paths.get(ciPluginDir.absolutePath, "docker-buildx"))
            println("docker-buildx准备完毕")
        }
    }

    private fun SlimContext.createBuildxIfNotExist() {
        val buildxLs = OsUtil.execCommand("docker", "buildx", "ls")
        if (buildxLs.exitCode != 0) {
            throw RuntimeException("执行docker buildx ls失败: ${buildxLs.result}")
        }

        if (!buildxLs.result.contains(getBuildxName())) {
            val buildxCreateResult = OsUtil.execShell(
                File(tarFiles.tempDir.absolutePath, "$serviceName-create-buildx.sh"),
                """
                    docker buildx create \
                        --name ${getBuildxName()} \
                        --driver-opt network=host \
                        --driver docker-container \
                        --bootstrap
                """.trimIndent()
            )

            if (buildxCreateResult.exitCode != 0) {
                throw RuntimeException("创建docker buildx容器失败: ${buildxCreateResult.result}")
            }
        }
        println("成功创建docker buildx容器")
    }

    private fun buildImage(
        tag: String,
        buildDir: String,
        dockerFile: String,
        outputFile: String,
        platform: String
    ): Int {
        OsUtil.execCommand("docker", "buildx", "use", getBuildxName())
        val result = OsUtil.execCommand(
            "docker", "buildx", "build",
            "-f", dockerFile,
            "-o", "type=docker,dest=$outputFile",
            "-t", tag,
            "--platform", platform,
            buildDir
        )
        return result.exitCode
    }

    private fun getImageIdFromTar(tarFile: File): String? {
        println("获取${tarFile.name}的镜像ID")
        val objectMapper = ObjectMapper()
        try {
            TarArchiveInputStream(tarFile.inputStream()).use { tarInput ->
                return readFile(tarInput, objectMapper)
            }
        } catch (e: Exception) {
            println("解析镜像ID失败: ${e.message}")
        }
        return null
    }

    private fun readFile(tarInput: TarArchiveInputStream, objectMapper: ObjectMapper): String? {
        var entry = tarInput.nextEntry
        while (entry != null) {
            if (entry.name == "manifest.json") {
                val manifestJson = tarInput.readBytes().toString(Charsets.UTF_8)
                val jsonNode = objectMapper.readValue(manifestJson, JsonNode::class.java)

                if (jsonNode.isArray and !jsonNode.isEmpty) {
                    val configFileName = jsonNode[0]["Config"].asText()
                    return configFileName.substringAfterLast("/").substringAfterLast(":")
                }
            }
            entry = tarInput.nextTarEntry
        }
        return null
    }

    private fun getBuildxName(): String = "devops-buildx"

    private fun Project.findPropertyOrDefault(name: String, default: String): String {
        return System.getProperty(name) ?: System.getenv(name) ?: run {
            if (hasProperty(name)) {
                return property(name).toString()
            } else {
                default
            }
        }
    }
}
