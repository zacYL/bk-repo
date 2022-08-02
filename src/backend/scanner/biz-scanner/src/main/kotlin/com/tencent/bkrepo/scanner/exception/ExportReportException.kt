package com.tencent.bkrepo.scanner.exception

import com.tencent.bkrepo.common.api.exception.ErrorCodeException
import com.tencent.bkrepo.scanner.message.ScannerMessageCode

class ExportReportException : ErrorCodeException(ScannerMessageCode.EXPORT_REPORT_FAIL)

