package com.tencent.bkrepo.scanner.exception

import com.tencent.bkrepo.common.api.exception.NotFoundException
import com.tencent.bkrepo.common.checker.message.ScanMessageCode

/**
 * 未匹配到制品
 */
class ArtifactNotMatchException(
    msg: String
) : NotFoundException(ScanMessageCode.ARTIFACT_NOT_FOUND, msg)
