package com.tencent.bkrepo.maven.exception

import com.tencent.bkrepo.common.api.exception.BadRequestException
import com.tencent.bkrepo.maven.enum.MavenMessageCode

class JarFormatException(error: String) : BadRequestException(MavenMessageCode.FILE_RESOLVE_FAILED, error)
