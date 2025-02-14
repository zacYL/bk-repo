package com.tencent.bkrepo.pypi.util

import com.tencent.bkrepo.pypi.constants.HTML_ENCODED_GREATER_THAN
import com.tencent.bkrepo.pypi.constants.HTML_ENCODED_LESS_THAN

object HtmlUtils {
    fun partialEncode(s: String) = s.replace("<", HTML_ENCODED_LESS_THAN).replace(">", HTML_ENCODED_GREATER_THAN)
}
