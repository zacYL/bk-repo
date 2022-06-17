package com.tencent.bkrepo.scanner.executor

import com.tencent.bkrepo.common.scanner.pojo.scanner.SubScanTaskStatus
import com.tencent.bkrepo.common.scanner.pojo.scanner.scanCodeCheck.scanner.ScancodeToolkitScanner
import com.tencent.bkrepo.scanner.executor.scancodeCheck.ScancodeToolkitExecutor
import com.tencent.bkrepo.scanner.executor.pojo.ScanExecutorTask
import org.junit.jupiter.api.Test
import org.junit.platform.commons.logging.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import java.io.File


@SpringBootTest(classes = [ScannerExecutorApplication::class])
class ScancodeToolkitExecutorTest {

    @Autowired
    private lateinit var scancodeToolkitExecutor: ScancodeToolkitExecutor

    //    @Test
    fun scanExec() {
        // 基础信息
        val sanPath = "D:\\tmp\\lucene-analyzers-common-8.9.0.jar"
        val rootPath = "\\scanTest"
        val imagesName = "scancode-toolkit:latest"
        val parentTaskId = "10000"
        val taskId = "000000"
        val sha256 = "1234567890"
        // 可将本地启容器，配置后进行调试
        val scanner = ScancodeToolkitScanner(
            name = "scancode_toolkit",
            version = "3.1",
            rootPath = rootPath,
            container = ScancodeToolkitScanner.ScancodeToolkitDockerImage(image = imagesName),
            cleanWorkDir = false
        )
        val task = ScanExecutorTask(
            taskId = taskId,
            parentTaskId = parentTaskId,
            scanner = scanner,
            inputStream = File(sanPath).inputStream(),
            sha256 = sha256
        )
        val result = scancodeToolkitExecutor.scan(task = task)
        result.overview
    }


    companion object {
        private val logger = LoggerFactory.getLogger(ScancodeToolkitExecutorTest::class.java)
    }
}