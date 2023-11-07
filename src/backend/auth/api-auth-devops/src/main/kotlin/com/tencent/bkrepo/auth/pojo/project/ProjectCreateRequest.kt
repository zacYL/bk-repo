package com.tencent.bkrepo.auth.pojo.project

data class ProjectCreateRequest(
    val typeId: String,
    val parentCode: String,
    val projectName: String,
    val englishNameCustom: String,
    val description: String,
    val deptId: String,
    val deptName: String,
    val relationProject: String,
    val relationDemand: String,
    val administrator: String,
    val roleTemplates: String,
    val props: List<PropsInfo>,
    val templateId: String,
    val projectCode: String
)

data class PropsInfo(
    val propId: String,
    val propName: String,
    val propTypeval: String = "INPUT",
    val values: Values,
    val customValues: List<String>
)
data class Values(
    val id: String,
    val propId: String,
    val option: String,
    val weight: Int = 0
)
