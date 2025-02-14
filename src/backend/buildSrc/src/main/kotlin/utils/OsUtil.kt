package utils

import java.io.File

object OsUtil {
    fun isLinux(): Boolean = System.getProperty("os.name").toLowerCase().contains("linux")

    fun execCommand(vararg command: String): CommandResult {
        try {
            val processBuilder = ProcessBuilder(*command)
            processBuilder.redirectErrorStream(true)
            val process = processBuilder.start()
            println("执行命令： ${command.joinToString(" ")}")
            val exitCode = process.waitFor()
            val result = process.inputStream
                .readBytes()
                .toString(Charsets.UTF_8)
                .trim()
            if (exitCode == 0) {
                exitCode to result
            } else {
                println("执行失败: ${command.joinToString(" ")}, 错误日志: $result")
                exitCode to ""
            }
            return CommandResult(exitCode, result)
        } catch (e: Exception) {
            return CommandResult(-1, "命令执行失败: ${e.message}\n${e.localizedMessage}")
        }
    }

    fun execShell(shellFile: File, shellContent: String? = null): CommandResult {
        shellContent?.let { shellFile.writeText(it) }
        FileUtil.chmodX(shellFile)

        return execCommand("sh", shellFile.absolutePath)
    }

    data class CommandResult(
        val exitCode: Int,
        val result: String
    )
}
