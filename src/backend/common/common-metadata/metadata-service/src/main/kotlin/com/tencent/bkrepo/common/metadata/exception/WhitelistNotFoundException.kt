package com.tencent.bkrepo.common.metadata.exception

import com.tencent.bkrepo.common.api.exception.NotFoundException
import com.tencent.bkrepo.common.api.message.CommonMessageCode

class WhitelistNotFoundException(
        whitelistId: String
) : NotFoundException(CommonMessageCode.RESOURCE_NOT_FOUND, whitelistId)
