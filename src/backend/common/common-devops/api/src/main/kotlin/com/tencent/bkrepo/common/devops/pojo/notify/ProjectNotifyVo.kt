package com.tencent.bkrepo.common.devops.pojo.notify

/**
 * 消息通知视图
 *
 * 已知模块ID对应的名称如下JSON：
 * 已知的模块发送消息时只需发送moduleId，不需附上moduleName, 如果没有则需带上moduleId + moduleName
 * [ 默认客户端发送消息存在moduleName则取客户端模块 ]
 *
 * {
 *     "PROJECT_ITEM": "项目集",
 *     "MOCK": "Mock服务模块",
 *     "FILE_MANAGE": "文档管理模块",
 *     "VERSION_EXP": "版本体验模块",
 *     "MEASURE_ANALYZE": "度量分析平台模块",
 *     "AUTO_TESTS": "自动化测试平台模块",
 *     "CODE_MANAGER": "代码管理平台模块",
 *     "QUALITY": "质量门禁平台模块",
 *     "CODECC": "代码检查平台模块",
 *     "PIPELINE_MEASURE": "流水线度量模块",
 *     "LIBRARY": "制品库模块",
 *     "CI": "持续集成平台模块",
 *     "TV": "敏捷协同模块"
 *  }
 *
 */

data class ProjectNotifyVo(
    // 事件发生的租户Id
    var tenantId: String?,
    // 事件发生的项目Id
    var projectId: String?,
    // 事件发生的模块Id
    var moduleId: String?,
    // 事件发生的模块名
    var moduleName: String?,
    // 事件发起人Id
    var promoterId: String?,
    // 事件接收人Id
    var receiverId: String?,
    // 事件操作时间
    var operationDate: Long?,
    /**
     * 事件消息, 支持多个跳转
     *
     * 消息例子：CodeCC模块触发定时扫描，任务test_java扫描失败
     * 消息中 "CodeCC模块" 为一个连接地址：http://xxx; "任务test_java" 为第二个连接地址：http://xxx
     * 则生产者发送的消息为格式为：${codecc}触发定时扫描，${taskId}扫描失败
     *
     * 其中占位符的变量(codecc、taskId)为operationAddressUrl参数所定义
     * MessageUrlInfo：
     * enName: codecc
     * cnName: CodeCC模块
     * url: http://xxx
     *
     * MessageUrlInfo：
     * enName: taskId
     * cnName: 任务test_java
     * url: http://xxx
     *
     */
    // 事件消息
    var operationMessgae: String?,
    // 英文消息
    var enOperationMessage: String? = null,
    // 事件地址URL
    var operationAddressUrl: List<MessageUrlInfo>? = null
)

class MessageUrlInfo(
    // 消息url英文名称（标识）
    val enName: String?,
    // 消息url中文名称
    val cnName: String?,
    // 跳转的Url地址
    val url: String?
)
