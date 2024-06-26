package com.tencent.bkrepo.common.artifact.exception

import com.tencent.bkrepo.common.api.exception.NotFoundException
import com.tencent.bkrepo.common.artifact.message.ArtifactMessageCode

class PathPatternNotMatchException(
    fullPath: String
) : NotFoundException(ArtifactMessageCode.PATH_PATTERN_NOT_MATCHED, fullPath)
