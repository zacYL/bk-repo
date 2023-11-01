package com.tencent.bkrepo.analyst.model

enum class ExportContent(val msgKey: String) {
    // ScanPlanExport
    SCAN_PLAN_STATUS_INIT("excel.scan-plan.status.init"),
    SCAN_PLAN_STATUS_RUNNING("excel.scan-plan.status.running"),
    SCAN_PLAN_STATUS_STOP("excel.scan-plan.status.stop"),
    SCAN_PLAN_STATUS_SUCCESS("excel.scan-plan.status.success"),
    SCAN_PLAN_STATUS_UN_QUALITY("excel.scan-plan.status.un-quality"),
    SCAN_PLAN_STATUS_QUALITY_PASS("excel.scan-plan.status.quality-pass"),
    SCAN_PLAN_STATUS_QUALITY_UN_PASS("excel.scan-plan.status.quality-un-pass"),
    SCAN_PLAN_STATUS_FAILED("excel.scan-plan.status.failed"),

    // LeakDetailExport
    LEAK_DETAIL_SEVERITY_CRITICAL("excel.leak-detail.severity.critical"),
    LEAK_DETAIL_SEVERITY_HIGH("excel.leak-detail.severity.high"),
    LEAK_DETAIL_SEVERITY_MEDIUM("excel.leak-detail.severity.medium"),
    LEAK_DETAIL_SEVERITY_LOW("excel.leak-detail.severity.low"),

    // LicenseScanDetailExport
    LICENSE_SCAN_DETAIL_OSI_TRUE("excel.license-scan-detail.osi.true"),
    LICENSE_SCAN_DETAIL_OSI_FALSE("excel.license-scan-detail.osi.false"),
    LICENSE_SCAN_DETAIL_FSF_TRUE("excel.license-scan-detail.fsf.true"),
    LICENSE_SCAN_DETAIL_FSF_FALSE("excel.license-scan-detail.fsf.false"),
    LICENSE_SCAN_DETAIL_DEPRECATED_TRUE("excel.license-scan-detail.deprecated.true"),
    LICENSE_SCAN_DETAIL_DEPRECATED_FALSE("excel.license-scan-detail.deprecated.false"),
    LICENSE_SCAN_DETAIL_COMPLIANCE_TRUE("excel.license-scan-detail.compliance.true"),
    LICENSE_SCAN_DETAIL_COMPLIANCE_FALSE("excel.license-scan-detail.compliance.false");
}
