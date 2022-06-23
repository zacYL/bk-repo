import Vue from 'vue'

const prefix = 'scanner/api/scan'

export default {
    // 创建扫描方案
    createScan (_, body) {
        return Vue.prototype.$ajax.post(
            `${prefix}/plan/create`,
            body
        )
    },
    // 扫描方案列表
    getScanList (_, { projectId, type, name, current = 1, limit = 20 }) {
        return Vue.prototype.$ajax.get(
            `${prefix}/plan/list/${projectId}`,
            {
                params: {
                    pageNumber: current,
                    pageSize: limit,
                    type: type || undefined,
                    name: name || undefined
                }
            }
        )
    },
    // 所有扫描方案
    getScanAll (_, { projectId, type }) {
        return Vue.prototype.$ajax.get(
            `${prefix}/plan/all/${projectId}`,
            {
                params: {
                    type
                }
            }
        )
    },
    // 获取方案基本设置
    getScanConfig (_, { projectId, id }) {
        return Vue.prototype.$ajax.get(
            `${prefix}/plan/detail/${projectId}/${id}`
        )
    },
    // 保存方案基本设置
    saveScanConfig (_, body) {
        return Vue.prototype.$ajax.post(
            `${prefix}/plan/update`,
            body
        )
    },
    // 删除扫描方案
    deleteScan (_, { projectId, id }) {
        return Vue.prototype.$ajax.delete(
            `${prefix}/plan/delete/${projectId}/${id}`
        )
    },
    // 报告基本信息
    scanReportOverview (_, params) {
        return Vue.prototype.$ajax.get(
            `${prefix}/plan/count`,
            {
                params
            }
        )
    },
    // 许可报告基本信息
    scanLicenseOverview (_, params) {
        return Vue.prototype.$ajax.get(
            `${prefix}/plan/license/count`,
            {
                params
            }
        )
    },
    // 报告制品列表
    scanReportList (_, { projectId, id, query, current = 1, limit = 20 }) {
        return Vue.prototype.$ajax.get(
            `${prefix}/plan/artifact`,
            {
                params: {
                    projectId,
                    id,
                    ...query,
                    pageNumber: current,
                    pageSize: limit
                }
            }
        )
    },
    // 中止扫描
    stopScan (_, { projectId, id, recordId }) {
        return Vue.prototype.$ajax.post(
            `${prefix}/${projectId}/stop`,
            null,
            {
                params: {
                    id,
                    recordId
                }
            }
        )
    },
    // 制品扫描报告基本信息
    artiReportOverview (_, { projectId, recordId, viewType, taskId }) {
        let url = `${prefix}/artifact/count/${projectId}/${recordId}`
        if (viewType === 'TASKVIEW') {
            url = `${prefix}/tasks/${taskId}/subtasks/${recordId}`
        }
        return Vue.prototype.$ajax.get(url)
    },
    // 许可扫描报告基本信息
    licenseReportOverview (_, { projectId, recordId, viewType, taskId }) {
        let url = `${prefix}/artifact/license/count/${projectId}/${recordId}`
        if (viewType === 'TASKVIEW') {
            url = `${prefix}/license/tasks/${taskId}/subtasks/${recordId}`
        }
        return Vue.prototype.$ajax.get(url)
    },
    // 制品扫描报告漏洞列表
    getLeakList (_, { projectId, recordId, viewType, vulId, severity, current = 1, limit = 20 }) {
        let url = `${prefix}/artifact/leak/${projectId}/${recordId}`
        if (viewType === 'TASKVIEW') {
            url = `${prefix}/reports/${recordId}`
        }
        return Vue.prototype.$ajax.get(
            url,
            {
                params: {
                    vulId: vulId || undefined,
                    leakType: severity || undefined,
                    pageNumber: current,
                    pageSize: limit
                }
            }
        )
    },
    // 许可扫描报告漏洞列表
    getLicenseLeakList (_, { projectId, recordId, viewType, licenseId, current = 1, limit = 20 }) {
        let url = `${prefix}/artifact/license/leak/${projectId}/${recordId}`
        if (viewType === 'TASKVIEW') {
            url = `${prefix}/license/reports/${recordId}`
        }
        return Vue.prototype.$ajax.get(
            url,
            {
                params: {
                    licenseId: licenseId || undefined,
                    pageNumber: current,
                    pageSize: limit
                }
            }
        )
    },
    // 批量扫描
    startScan (_, body) {
        return Vue.prototype.$ajax.post(
            `${prefix}`,
            body
        )
    },
    // 单个扫描
    startScanSingle (_, { projectId, id, repoName, version, packageKey, fullPath }) {
        // return Vue.prototype.$ajax.post(
        //     `${prefix}/single`,
        //     body
        // )
        return Vue.prototype.$ajax.post(
            `${prefix}`,
            {
                id,
                force: true,
                rule: {
                    rules: [
                        {
                            field: 'projectId',
                            value: projectId,
                            operation: 'EQ'
                        },
                        {
                            field: 'repoName',
                            value: [repoName],
                            operation: 'IN'
                        },
                        {
                            rules: [
                                {
                                    rules: [
                                        packageKey
                                            ? {
                                                field: 'version',
                                                operation: 'EQ',
                                                value: version
                                            }
                                            : undefined,
                                        packageKey
                                            ? {
                                                field: 'key',
                                                operation: 'EQ',
                                                value: packageKey
                                            }
                                            : undefined,
                                        !packageKey
                                            ? {
                                                field: 'fullPath',
                                                operation: 'EQ',
                                                value: fullPath
                                            }
                                            : undefined
                                    ].filter(Boolean),
                                    relation: 'AND'
                                }
                            ],
                            relation: 'OR'
                        }
                    ],
                    relation: 'AND'
                }
            }
        )
    },
    // 制品关联的扫描方案
    getArtiScanList (_, { projectId, repoType, repoName, packageKey, version, fullPath }) {
        return Vue.prototype.$ajax.get(
            `${prefix}/plan/relation/artifact`,
            {
                params: {
                    projectId,
                    repoType: repoType.toUpperCase() || undefined,
                    repoName,
                    packageKey: packageKey || undefined,
                    version: version || undefined,
                    fullPath: fullPath || undefined
                }
            }
        )
    },
    // 获取扫描器列表
    getScannerList () {
        return Vue.prototype.$ajax.get('/scanner/api/scanners/base')
    },
    // 获取质量规则
    getQualityRule (_, { id }) {
        return Vue.prototype.$ajax.get(`/scanner/api/scan/quality/${id}`)
    },
    // 更新质量规则
    saveQualityRule (_, { id, body }) {
        return Vue.prototype.$ajax.post(`/scanner/api/scan/quality/${id}`, body)
    },
    // 查询任务列表
    getScanTaskList (_, { projectId, planId, triggerType, namePrefix, current = 1, limit = 20 }) {
        return Vue.prototype.$ajax.get(
            `${prefix}/tasks`,
            {
                params: {
                    projectId,
                    planId,
                    triggerType,
                    namePrefix,
                    pageNumber: current,
                    pageSize: limit
                }
            }
        )
    },
    // 任务制品列表
    scanTaskReportList (_, { projectId, taskId, id, query, current = 1, limit = 20 }) {
        if (!taskId) return Promise.resolve({ records: [], totalRecords: 0 })
        return Vue.prototype.$ajax.get(
            `${prefix}/tasks/${taskId}/subtasks`,
            {
                params: {
                    projectId,
                    id,
                    ...query,
                    pageNumber: current,
                    pageSize: limit
                }
            }
        )
    },
    // 任务制品列表
    stopScanTask (_, { projectId, taskId }) {
        return Vue.prototype.$ajax.post(
            `${prefix}/${projectId}/tasks/${taskId}/stop`
        )
    },
    // 查询许可证列表
    getLicenseList (_, { name, isTrust, current = 1, limit = 20 }) {
        return Vue.prototype.$ajax.get(
            'scanner/api/license/list',
            {
                params: {
                    name,
                    isTrust,
                    pageNumber: current,
                    pageSize: limit
                }
            }
        )
    },
    // 设置许可证
    editLicense (_, { licenseId, isTrust }) {
        return Vue.prototype.$ajax.post(
            `scanner/api/license/${licenseId}`,
            {
                isTrust
            }
        )
    }
}
