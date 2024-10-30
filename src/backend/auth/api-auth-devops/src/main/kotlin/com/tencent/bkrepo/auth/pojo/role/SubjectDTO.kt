package com.tencent.bkrepo.auth.pojo.role

import com.tencent.bkrepo.auth.constant.AuthSubjectCode
import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty

@ApiModel("授权主体")
class SubjectDTO(
    @ApiModelProperty("授权主体ID")
    val subjectId: String,
    @ApiModelProperty("授权主体标识")
    val subjectCode: String
) {
    companion object {
        fun user(userId: String): SubjectDTO {
            return SubjectDTO(
                subjectId = userId,
                subjectCode = AuthSubjectCode.USER
            )
        }

        fun role(roleId: String): SubjectDTO {
            return SubjectDTO(
                subjectId = roleId,
                subjectCode = AuthSubjectCode.DEPARTMENT
            )
        }

        fun group(groupId: String): SubjectDTO {
            return SubjectDTO(
                subjectId = groupId,
                subjectCode = AuthSubjectCode.GROUP
            )
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        val obj = other as SubjectDTO
        return subjectCode == obj.subjectCode && subjectId == obj.subjectId
    }

    // 重写 hashCode() 方法
    override fun hashCode(): Int {
        var result = subjectCode.hashCode()
        result = 31 * result + subjectId.hashCode()
        return result
    }
}
