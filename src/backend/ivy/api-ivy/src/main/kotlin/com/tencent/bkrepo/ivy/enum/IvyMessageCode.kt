package com.tencent.bkrepo.ivy.enum

import com.tencent.bkrepo.common.api.message.MessageCode

enum class IvyMessageCode(private val key: String) : MessageCode {
    IVY_ARTIFACT_FORMAT_ERROR("ivy.artifact.format.error"),
    IVY_REQUEST_FORBIDDEN("ivy.request.forbidden"),
    IVY_ARTIFACT_UPLOAD("ivy.artifact.upload"),
    IVY_ARTIFACT_NOT_FOUND("ivy.artifact.not.found"),
    FILE_RESOLVE_FAILED("file.resolve.failed"),
    PARAMETER_CONTAINS_INVALID("parameter.contains.invalid"),
    PARAMETER_EXPECT("parameter.expect"),
    IVY_ARTIFACT_COVER_FORBIDDEN("ivy.artifact.cover.forbidden"),
    IVY_ARTIFACT_DELETE_FORBIDDEN("ivy.artifact.delete.forbidden"),
    IVY_VERSION_NOT_FOUND("ivy.version.not.found"),
    IVY_VERSION_RESOLVE_FAILED("ivy.version.resolve.failed")
    ;

    override fun getBusinessCode() = ordinal + 1
    override fun getKey() = key
    override fun getModuleCode() = 28
}
