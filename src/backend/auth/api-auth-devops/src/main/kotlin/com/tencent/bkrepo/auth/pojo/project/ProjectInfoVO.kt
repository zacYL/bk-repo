package com.tencent.bkrepo.auth.pojo.project

import io.swagger.v3.oas.annotations.media.Schema

@Suppress("ALL")
@Schema(description = "项目-显示模型")
data class ProjectInfoVO(
    @Schema(description = "主键ID")
    val id: Long,
    @Schema(description = "类别ID")
    val typeId: String? = "",
    @Schema(description = "项目ID")
    // @JsonProperty("project_id")
    val projectId: String,
    @Schema(description = "项目名称")
    // @JsonProperty("project_name")
    val projectName: String,
    @Schema(description = "项目代码")
    // @JsonProperty("project_code")
    val projectCode: String,
    @Schema(description = "项目类型")
    // @JsonProperty("project_type")
    val projectType: Int?,
    @Schema(description = "审批状态")
    // @JsonProperty("approval_status")
    val approvalStatus: Int?,
    @Schema(description = "审批时间")
    // @JsonProperty("approval_time")
    val approvalTime: String?,
    @Schema(description = "审批人")
    val approver: String?,
    @Schema(description = "cc业务ID")
    // @JsonProperty("cc_app_id")
    val ccAppId: Long?,
    @Schema(description = "cc业务名称")
    // @JsonProperty("cc_app_name")
    val ccAppName: String?,
    @Schema(description = "创建时间")
    // @JsonProperty("created_at")
    val createdAt: String?,
    @Schema(description = "创建人")
    val creator: String?,
    @Schema(description = "数据ID")
    // @JsonProperty("data_id")
    val dataId: Long?,
    @Schema(description = "部署类型")
    // @JsonProperty("deploy_type")
    val deployType: String?,
    @Schema(description = "事业群ID")
    // @JsonProperty("bg_id")
    val bgId: String?,
    @Schema(description = "事业群名字")
    // @JsonProperty("bg_name")
    val bgName: String?,
    @Schema(description = "中心ID")
    // @JsonProperty("center_id")
    val centerId: String?,
    @Schema(description = "中心名称")
    // @JsonProperty("center_name")
    val centerName: String?,
    @Schema(description = "部门ID")
    // @JsonProperty("dept_id")
    val deptId: String?,
    @Schema(description = "部门名称")
    // @JsonProperty("dept_name")
    val deptName: String?,
    @Schema(description = "描述")
    val description: String?,
    @Schema(description = "英文缩写")
    // @JsonProperty("english_name")
    val englishName: String,
    @Schema(description = "英文缩写,自定义仅展示")
    // @JsonProperty("english_name")
    val englishNameCustom: String,
    @Schema(description = "extra")
    val extra: String?,
    @Schema(description = "是否离线")
//    @get:JsonProperty("is_offlined")
    val offlined: Boolean?,
    @Schema(description = "是否保密")
//    @get:JsonProperty("is_secrecy")
    val secrecy: Boolean?,
    @Schema(description = "是否启用图表激活")
//    @get:JsonProperty("is_helm_chart_enabled")
    val helmChartEnabled: Boolean?,
    @Schema(description = "kind")
    val kind: Int?,
    @Schema(description = "logo地址")
    // @JsonProperty("logo_addr")
    val logoAddr: String?,
    @Schema(description = "评论")
    val remark: String?,
    @Schema(description = "修改时间")
    // @JsonProperty("updated_at")
    val updatedAt: String?,
    @Schema(description = "useBK")
    // @JsonProperty("use_bk")
    val useBk: Boolean?,
    @Schema(description = "启用")
    val enabled: Boolean?,
    @Schema(description = "是否灰度")
    val gray: Boolean,
    @Schema(description = "混合云CC业务ID")
    val hybridCcAppId: Long?,
    @Schema(description = "支持构建机访问外网")
    val enableExternal: Boolean?,
    @Schema(description = "支持IDC构建机")
    val enableIdc: Boolean? = false,
    @Schema(description = "流水线数量上限")
    val pipelineLimit: Int? = 500,
    @Deprecated("即将作废，兼容插件中被引用到的旧的字段命名，请用hybridCcAppId代替")
    @Schema(description = "混合云CC业务ID(即将作废，兼容插件中被引用到的旧的字段命名，请用hybridCcAppId代替)")
    val hybrid_cc_app_id: Long?,
    @Deprecated("即将作废，兼容插件中被引用到的旧的字段命名，请用projectId代替")
    @Schema(description = "项目ID(即将作废，兼容插件中被引用到的旧的字段命名，请用projectId代替)")
    val project_id: String?,
    @Deprecated("即将作废，兼容插件中被引用到的旧的字段命名，请用projectName代替")
    @Schema(description = "旧版项目名称(即将作废，兼容插件中被引用到的旧的字段命名，请用projectName代替)")
    val project_name: String?,
    @Deprecated("即将作废，兼容插件中被引用到的旧的字段命名，请用projectCode代替")
    @Schema(description = "旧版项目代码(即将作废，兼容插件中被引用到的旧的字段命名，请用projectCode代替)")
    val project_code: String?,
    @Deprecated("即将作废，兼容插件中被引用到的旧的字段命名，请用ccAppId代替")
    @Schema(description = "旧版cc业务ID(即将作废，兼容插件中被引用到的旧的字段命名，请用ccAppId代替)")
    val cc_app_id: Long?,
    @Schema(description = "旧版cc业务名称(即将作废，兼容插件中被引用到的旧的字段命名，请用ccAppName代替)")
    @Deprecated("即将作废，兼容插件中被引用到的旧的字段命名，请用ccAppName代替")
    val cc_app_name: String?,
    @Schema(description = "项目路由指向")
    val routerTag: String?,
    @Schema(description = "关联系统Id")
    val relationId: String?,
    @Schema(description = "项目是否被收藏")
    val star: Boolean = false,
    @Schema(description = "父级项目标识")
    val parentCode: String? = null,
    @Schema(description = "租户ID")
    var tenantId: String? = null,
    @Schema(description = "租户CODE")
    var tenantCode: String? = null,
    @Schema(description = "租户名称")
    var tenantName: String? = null
)
