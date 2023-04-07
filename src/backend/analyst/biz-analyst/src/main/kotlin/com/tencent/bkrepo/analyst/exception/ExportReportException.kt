package com.tencent.bkrepo.analyst.exception

import com.tencent.bkrepo.analyst.message.ScannerMessageCode
import com.tencent.bkrepo.common.api.exception.ErrorCodeException

class ExportReportException : ErrorCodeException(ScannerMessageCode.EXPORT_REPORT_FAIL)

