package com.tencent.bkrepo.scanner.config

import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component

@Component
class ExecutorConfig {

    /**
     * 任务执行根目录
     */
    @Value("\${executor.scan.rootDir:}")
    var rootDir: String = ""

    /**
     * 配置模板路径
     */
    @Value("\${executor.scan.configTemplateDir:}")
    var configTemplateDir: String = ""

    /**
     * 输出路径
     */
    @Value("\${executor.scan.outputDir:}")
    var outputDir: String = ""

    /**
     * 配置文件路径
     */
    @Value("\${executor.scan.configName:}")
    var configName: String = ""

    /**
     * 输入路径
     */
    @Value("\${executor.scan.inputDir:}")
    var inputDir: String = ""

    /**
     * 开启启动时全量扫描
     */
    @Value("\${executor.scan.full:false}")
    var full: Boolean = false

    /**
     * 是否删除运行时
     */
    @Value("\${executor.scan.clean:false}")
    var clean: Boolean = true
}
