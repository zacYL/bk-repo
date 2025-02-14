package com.tencent.bkrepo.generic.exception

data class GenericExceptionResponse(
    val status: String,
    val error: String?
)