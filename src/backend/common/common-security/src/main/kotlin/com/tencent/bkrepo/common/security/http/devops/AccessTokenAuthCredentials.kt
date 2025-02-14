package com.tencent.bkrepo.common.security.http.devops

import com.tencent.bkrepo.common.security.http.credentials.HttpAuthCredentials

data class AccessTokenAuthCredentials(val userId: String?, val token: String) : HttpAuthCredentials
