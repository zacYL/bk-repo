import java.nio.file.Paths

tasks.register("slim") {
    dependsOn("copyToRelease")
    val serviceName = "repo" + project.name.substringAfter("boot-")
                .substringBefore("-registry")
                .substringBefore("-canway")
    val serviceHome = Paths.get(project.rootDir.absolutePath, "release", serviceName).toFile()

    doLast {
        val slimContext = utils.SlimContext(project, serviceName, serviceHome)
        slimContext.execute()
    }
}
