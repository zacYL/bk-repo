package com.tencent.bkrepo.common.scanner.pojo.scanner.scanCodeCheck.scanner

import com.tencent.bkrepo.common.scanner.pojo.scanner.Scanner
import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty

@ApiModel("scancode_toolkit(licenses扫描)扫描器配置")
class ScancodeToolkitScanner(
    override val name: String,
    @ApiModelProperty("扫描器版本")
    override val version: String,
    @ApiModelProperty("扫描器根目录")
    val rootPath: String,
    @ApiModelProperty("扫描器配置文件路径，相对于工作目录")
    val configBashPath: String = DEFAULT_CONFIG_BASH_PATH,
    @ApiModelProperty("扫描结束后是否清理工作目录")
    val cleanWorkDir: Boolean = true,
    @ApiModelProperty("最大允许的扫描时间")
    val maxScanDuration: Long = DEFAULT_MAX_SCAN_DURATION,
    @ApiModelProperty("使用的容器镜像")
    val container: ScancodeToolkitDockerImage
) : Scanner(name, TYPE, version) {
    companion object {
        /**
         * 扫描器和漏洞库版本号分隔符
         */
        const val VERSION = ""
        const val TYPE = "scancodeToolkit"
        const val DEFAULT_MAX_SCAN_DURATION = 10 * 60 * 1000L
        const val DEFAULT_CONFIG_BASH_PATH = "/toolScan.sh"
    }


    @ApiModel("arrowhead容器镜像配置")
    data class ScancodeToolkitDockerImage(
        @ApiModelProperty("使用的镜像名和版本")
        val image: String,
        @ApiModelProperty("容器启动参数")
        val args: String = "",
        @ApiModelProperty("容器内的工作目录")
        val workDir: String = "/data",
        @ApiModelProperty("输入目录，相对于workDir的路径")
        val inputDir: String = "/package",
        @ApiModelProperty("输出目录，相对于workDir的路径")
        val outputDir: String = "/output"
    )

}
