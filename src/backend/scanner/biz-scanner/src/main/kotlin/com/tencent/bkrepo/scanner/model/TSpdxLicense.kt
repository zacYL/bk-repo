package com.tencent.bkrepo.scanner.model

import org.springframework.data.mongodb.core.mapping.Document
import java.time.LocalDateTime

@Document("spdx_license")
//TODO 设置索引
data class TSpdxLicense(
    var id: String? = null,
    var createdBy: String,
    var createdDate: LocalDateTime,
    var lastModifiedBy: String,
    var lastModifiedDate: LocalDateTime,

    /**
     * 许可证名称
     */
    var name: String,

    /**
     * 许可证标识符
     */
    var licenseId: String,

    /**
     * 指向其他许可证副本的交叉引用 URL
     */
    var seeAlso: MutableList<String>,

    /**
     * 对许可证文件的 HTML 格式的引用
     */
    var reference: String,

    /**
     * 是否被弃用
     */
    var isDeprecatedLicenseId: Boolean,

    /**
     *  OSI是否已批准许可证
     */
    var isOsiApproved: Boolean,

    /**
     * 是否FSF认证免费
     */
    var isFsfLibre: Boolean? = null,

    /**
     * 包含许可证详细信息的 JSON 文件的 URL
     */
    var detailsUrl: String,

    /**
     * 是否信任
     */
    var isTrust: Boolean = true,

    /**
     * 风险等级
     */
    var risk: String? = null
)
