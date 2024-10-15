package utils

import org.gradle.api.Project
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.nio.file.Files
import java.nio.file.Paths
import java.nio.file.StandardCopyOption
import java.time.LocalDateTime
import java.util.Base64

class SlimContext(
    val project: Project,
    val serviceName: String,
    private val serviceHome: File
) {
    private val packageFiles = PackageFiles(
        templatesFilesDir = Paths.get(project.rootDir.absolutePath, "../../support-files/templates/canway").toFile(),
        scriptFilesDir = Paths.get(project.rootDir.absolutePath, "script").toFile(),
        sqlFilesDir = Paths.get(project.rootDir.absolutePath, "../../support-files/sql").toFile(),
        shareFilesDir = Paths.get(project.rootDir.absolutePath, "../../support-files/share").toFile(),
        helmChartsFilesDir = Paths.get(project.rootDir.absolutePath, "helm-charts/templates/cpack").toFile()
    )

    val tarFiles = TarFiles(
        ciDir = Paths.get(serviceHome.absolutePath, "ci").toFile(),
        k8sDir = Paths.get(serviceHome.absolutePath, "ci-k8s").toFile(),
        serviceDir = Paths.get(serviceHome.absolutePath, "ci", serviceName).toFile(),
        libDir = Paths.get(serviceHome.absolutePath, "ci", serviceName, "BOOT-INF/lib").toFile(),
        jarPublicDir = Paths.get(serviceHome.absolutePath, "ci", "jars-public").toFile(),
        jarPrivateDir = Paths.get(serviceHome.absolutePath, "ci", serviceName, "jars-private").toFile(),
        k8sLibDir = Paths.get(serviceHome.absolutePath, "ci-docker", serviceName, "BOOT-INF/lib").toFile(),
        k8sJarPublicDir = Paths.get(serviceHome.absolutePath, "ci-docker", serviceName, "jars-public").toFile(),
        k8sJarPrivateDir = Paths.get(serviceHome.absolutePath, "ci-docker", serviceName, "jars-private").toFile(),
        dockerDir = Paths.get(serviceHome.absolutePath, "ci-docker").toFile(),
        tempDir = Paths.get(serviceHome.absolutePath, "temp").toFile()
    )

    fun execute() {
        if (isK8s()) {
            println("当前编译模式为: K8S")
        } else {
            println("当前编译模式为: VM")
        }
        prepareDir()
        slim()
        if (isK8s()) {
            packageK8S()
        } else {
            packageVM()
        }
//        serviceHome.deleteRecursively()
    }

    private fun prepareDir() {
        tarFiles.ciDir.deleteRecursively()
        tarFiles.ciDir.mkdirs()
        tarFiles.libDir.mkdirs()
        tarFiles.jarPrivateDir.mkdirs()
        tarFiles.jarPublicDir.mkdirs()
        if (isK8s()) {
            tarFiles.k8sDir.deleteRecursively()
            tarFiles.k8sDir.mkdirs()
            Paths.get(tarFiles.k8sDir.absolutePath, serviceName).toFile().mkdirs()
            tarFiles.dockerDir.deleteRecursively()
            tarFiles.dockerDir.mkdirs()
            tarFiles.tempDir.mkdirs()
        }
    }

    private fun slim() {
        val jarFile = Paths.get(project.rootDir.absolutePath, "release", "${project.name}.jar").toFile()
        tarFiles.serviceDir.mkdirs()
        FileUtil.unzip(jarFile, tarFiles.serviceDir)
        val result = handleSymbolicLink()
        File(tarFiles.serviceDir.absolutePath, "jars-public-depend-list").writeText(result.first.toString())
        File(tarFiles.serviceDir.absolutePath, "jars-private-depend-list").writeText(result.second.toString())
        File(tarFiles.serviceDir.absolutePath, "build_info").writeText(getBuildInfo())
    }

    private fun packageVM() {
        println("VM: $serviceName")
        copyShare(tarFiles.ciDir.absolutePath)
        copySql(tarFiles.ciDir.absolutePath)
        copyTemplates(tarFiles.ciDir.absolutePath)
        renderBootService(tarFiles.ciDir.absolutePath)
        val packageFile = File(serviceHome, "../$serviceName-vm-${getBuildVersion()}.tar.gz")
        FileUtil.compressToTarGz(tarFiles.ciDir, packageFile)
    }

    private fun packageK8S() {
        println("K8S: $serviceName")
        println("准备Docker构建环境")
        FileUtil.copyDir(tarFiles.ciDir.toPath(), tarFiles.dockerDir.toPath())
        clearDuplicatedPublicJar()
        println("准备support-files目录")
        copyShare(tarFiles.k8sDir.absolutePath)
        copySql(tarFiles.k8sDir.absolutePath)
        copyTemplates(tarFiles.k8sDir.absolutePath)
        println("渲染K8s相关文件")
        renderK8sFile()
        DockerUtil.buildImage(this)
        println("压缩${serviceName}容器部署包")
        val k8sPackageFile = File(serviceHome, "../$serviceName-k8s-${getBuildVersion()}.tar.gz")
        FileUtil.compressToTarGz(tarFiles.k8sDir, k8sPackageFile)
        println("${serviceName}Kubernetes部署包位于: ${k8sPackageFile.absolutePath}\n\n")
    }

    private fun handleSymbolicLink(): Pair<StringBuilder, StringBuilder> {
        val jarsPublicDependencies = StringBuilder()
        val jarsPrivateDependencies = StringBuilder()
        val libDir = tarFiles.libDir
        val jarPrivateDir = tarFiles.jarPrivateDir
        val jarPublicDir = tarFiles.jarPublicDir
        val leafModules = getLeafModules(project.rootProject)
        libDir.listFiles()?.forEach { libJar ->
            val md5 = FileUtil.md5(libJar)
            val md5JarName = "${libJar.nameWithoutExtension}--$md5.jar"
            if (leafModules.any { module -> libJar.name.contains(module, ignoreCase = true) }) {
                jarsPrivateDependencies.append(md5JarName).append("\n")
                Files.move(libJar.toPath(), jarPrivateDir.resolve(md5JarName).toPath(), StandardCopyOption.REPLACE_EXISTING)
                Files.createSymbolicLink(libJar.toPath(), Paths.get("../../jars-private/$md5JarName"))
            } else {
                jarsPublicDependencies.append(md5JarName).append("\n")
                Files.move(libJar.toPath(), jarPublicDir.resolve(md5JarName).toPath(), StandardCopyOption.REPLACE_EXISTING)
                Files.createSymbolicLink(libJar.toPath(), Paths.get("../../../jars-public/$md5JarName"))
            }
        }
        return Pair(jarsPublicDependencies, jarsPrivateDependencies)
    }

    private fun copyShare(targetPath: String) {
        Paths.get(targetPath, "support-files", "share").toFile().mkdirs()
        packageFiles.shareFilesDir.listFiles()?.forEach {
            FileUtil.copyDir(it.toPath(), Paths.get(targetPath, "support-files", "share", it.name))
        }
    }

    private fun copySql(targetPath: String) {
        Paths.get(targetPath, "support-files", "sql").toFile().mkdirs()
        packageFiles.sqlFilesDir.listFiles { _, name ->
            (name.endsWith(".sql") && name.contains(serviceName)) || name.contains("0001")
        }?.forEach {
            Files.copy(it.toPath(), Paths.get(targetPath, "support-files", "sql", it.name))
        }
    }

    private fun copyTemplates(targetPath: String) {
        Paths.get(targetPath, "support-files", "templates").toFile().mkdirs()
        packageFiles.templatesFilesDir.listFiles { _, name ->
            name.contains(serviceName) || name.equals("#etc#bkrepo#common.yaml")
        }?.forEach {
            Files.copy(it.toPath(), Paths.get(targetPath, "support-files", "templates", it.name))
        }
    }

    private fun copyHelmCharts(targetPath: String) {
        Paths.get(targetPath, "helm-charts", "templates", serviceName).toFile().mkdirs()
        packageFiles.helmChartsFilesDir.listFiles { _, name -> name.endsWith(".yaml") }?.forEach {
            val newFile = Paths.get(targetPath, "helm-charts", "templates", serviceName, "$serviceName-${it.name}").toFile()
            renderHelmChart(it, newFile)
        }
    }

    private fun renderBootService(targetPath: String) {
        // 渲染boot-service.sh
        val tplTpl = Paths.get(packageFiles.templatesFilesDir.absolutePath, "boot-reposervice.sh").toFile()
        val tplName = "$serviceName#boot-$serviceName.sh"
        // 新文件
        val tplFile = Paths.get(targetPath, "support-files", "templates", tplName).toFile()
        if (tplTpl.exists()) {
            tplFile.writeText(
                tplTpl.readText()
                    .replace("bkciservice", serviceName)
                    .replace("BKCISERVICE", serviceName.toUpperCase())
                    .replace("bkreposervice", serviceName)
                    .replace("BKREPOSERVICE", serviceName.toUpperCase())
            )
        }
    }

    private fun renderHelmChart(originFile: File, newFile: File) {
        // 新文件
        if (!newFile.exists()) newFile.createNewFile()
        newFile.writeText(originFile.readText().replace("cpack", serviceName))
    }

    private fun renderK8sFile() {
        // dockerfile
        val dockerfile = Paths.get(packageFiles.scriptFilesDir.absolutePath, "backend_private.Dockerfile").toFile()
        // 渲染boot-service.sh
        val tplName = "${serviceName}_private.Dockerfile"
        // 新文件
        val tplFile = Paths.get(tarFiles.dockerDir.absolutePath, tplName).toFile()
        if (dockerfile.exists()) {
            tplFile.writeText(
                dockerfile.readText()
                    .replace("cpack", serviceName)
            )
        }
        // backend.sh
        val backendSH = Paths.get(packageFiles.scriptFilesDir.absolutePath, "backend.sh").toFile()
        Files.copy(backendSH.toPath(), Paths.get(tarFiles.dockerDir.absolutePath, serviceName, backendSH.name))
        // helm-chart
        val serviceYml = Paths.get(packageFiles.templatesFilesDir.absolutePath, "helm-charts#build#cpack.yaml").toFile()
        val serviceNewName = "helm-charts#build#$serviceName.yaml"
        val newServiceYml = Paths.get(tarFiles.k8sDir.absolutePath, "support-files", "templates", serviceNewName).toFile()
        newServiceYml.writeText(
            serviceYml.readText()
                .replace("cpack", serviceName)
                .replace("CPACK", serviceName.toUpperCase())
        )
        copyHelmCharts(tarFiles.k8sDir.absolutePath)
    }

    private fun clearDuplicatedPublicJar() {
        val relativePath = project.findPropertyOrEmpty("JARS_PUBLIC_PATH")
        if (relativePath.isBlank()) {
            throw RuntimeException("JARS_PUBLIC_PATH不能为空")
        }
        println("开始下载JarsPublic清单: $relativePath")
        val publicManifest = downloadFromCustom(relativePath)
        val publicJars = publicManifest.readText().lines().toSet()
        println("移除ci-docker/jars-public目录下的重复依赖")
        File(tarFiles.dockerDir, "jars-public").listFiles()?.forEach { publicJar ->
            if (publicJars.contains(publicJar.name)) {
                publicJar.delete()
            }
        }
    }

    fun downloadFromCustom(path: String): File {
        val tempDir = Paths.get(serviceHome.absolutePath, "temp").toFile()
        tempDir.mkdirs()
        val downloadFile = File(tempDir, path.substringAfterLast("/"))
        if (downloadFile.exists()) {
            println("文件:${downloadFile.absolutePath}已存在，跳过下载")
            return downloadFile
        }
        val host = project.findPropertyOrDefault("devops_repo_host", "https://bkrepo.cwoa.net")
        val username = project.findPropertyOrDefault("devops_username", "admin")
        val accessToken = project.findPropertyOrDefault("devops_access_token", "bkrepo")
        val devopsProjectId = project.findPropertyOrDefault("devops_project_id", "devops")

        val basicHeaderValue = "Basic " + Base64.getEncoder().encodeToString("$username:$accessToken".toByteArray())

        val url = URL("$host/api/generic/$devopsProjectId/custom/${path.trim('/')}")
        val connection = url.openConnection() as HttpURLConnection
        connection.setRequestProperty("Authorization", basicHeaderValue)
        connection.requestMethod = "GET"

        val responseCode = connection.responseCode
        if (responseCode != HttpURLConnection.HTTP_OK) {
            throw RuntimeException("下载制品[$path]失败: ${connection.errorStream.bufferedReader().use { it.readText() }}")
        }
        connection.responseMessage
        // 将输入流写入文件
        connection.inputStream.use { inputStream ->
            FileOutputStream(downloadFile).use { outputStream ->
                inputStream.copyTo(outputStream)
            }
        }
        return downloadFile
    }

    private fun Project.findPropertyOrEmpty(name: String): String {
        return System.getProperty(name) ?: System.getenv(name) ?: run {
            if (hasProperty(name)) {
                return property(name).toString()
            } else {
                ""
            }
        }
    }

    private fun Project.findPropertyOrDefault(name: String, default: String): String {
        return System.getProperty(name) ?: System.getenv(name) ?: run {
            if (hasProperty(name)) {
                return property(name).toString()
            } else {
                default
            }
        }
    }

    private fun getLeafModules(project: Project): List<String> {
        return project.subprojects
            .filter { it.subprojects.isEmpty() }
            .map { it.name }
            .plus(project.subprojects.filter { it.subprojects.isNotEmpty() }.flatMap { getLeafModules(it) })
    }

    private fun getBuildInfo(): String {
        val buildInfo = StringBuilder()
        buildInfo.append("Version: ").append(getBuildVersion()).append("\n")
        buildInfo.append("Build-ID: ").append(getBuildId()).append("\n")
        buildInfo.append("Build-Num: ").append(getBuildNum()).append("\n")
        buildInfo.append("Pipeline-ID: ").append(getPipelineId()).append("\n")
        buildInfo.append("Build-Time: ").append(LocalDateTime.now().toString()).append("\n")

        buildInfo.append("Commit-ID: ").append(getCommitId()).append("\n")
        buildInfo.append("Branch: ").append(getCurrentBranch()).append("\n")

        return buildInfo.toString()
    }

    private fun getPipelineId(): String = project.findPropertyOrEmpty("BK_CI_PIPELINE_ID")

    private fun getBuildId(): String = project.findPropertyOrEmpty("BK_CI_BUILD_ID")

    private fun getBuildNum(): String = project.findPropertyOrEmpty("BK_CI_BUILD_NUM")

    fun getBuildVersion(): String = project.findPropertyOrEmpty("version")

    private fun getCommitId(): String = OsUtil.execCommand("git", "rev-parse", "--short", "HEAD").result

    private fun getCurrentBranch(): String = OsUtil.execCommand("git", "symbolic-ref", "--short", "HEAD").result

    private fun isK8s(): Boolean = project.findPropertyOrEmpty("devops.assemblyMode") == "KUBERNETES"

    data class PackageFiles(
        val templatesFilesDir: File,
        val scriptFilesDir: File,
        val sqlFilesDir: File,
        val shareFilesDir: File,
        val helmChartsFilesDir: File
    )

    data class TarFiles(
        val ciDir: File,
        val k8sDir: File,
        val serviceDir: File,
        val libDir: File,
        val jarPublicDir: File,
        val jarPrivateDir: File,
        val k8sLibDir: File,
        val k8sJarPublicDir: File,
        val k8sJarPrivateDir: File,
        val dockerDir: File,
        val tempDir: File
    )
}
