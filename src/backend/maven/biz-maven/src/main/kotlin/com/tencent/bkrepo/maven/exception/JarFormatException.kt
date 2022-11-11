package com.tencent.bkrepo.maven.exception

import com.tencent.bkrepo.common.api.exception.BadRequestException
import com.tencent.bkrepo.maven.message.MavenMessageCode

class JarFormatException(error: String) : BadRequestException(MavenMessageCode.FILE_RESOLVE_FAILED, error)
