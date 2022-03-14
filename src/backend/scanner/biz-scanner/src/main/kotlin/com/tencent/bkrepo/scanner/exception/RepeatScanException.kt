package com.tencent.bkrepo.scanner.exception

import com.tencent.bkrepo.common.api.exception.NotFoundException
import com.tencent.bkrepo.common.checker.message.ScanMessageCode

class RepeatScanException(
    msg: String
) : NotFoundException(ScanMessageCode.REPEAT_SCAN_ARTIFACT, msg)
