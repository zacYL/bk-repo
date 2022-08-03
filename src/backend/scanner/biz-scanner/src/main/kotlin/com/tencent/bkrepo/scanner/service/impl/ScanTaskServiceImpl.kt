/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
 *
 * Copyright (C) 2022 THL A29 Limited, a Tencent company.  All rights reserved.
 *
 * BK-CI 蓝鲸持续集成平台 is licensed under the MIT license.
 *
 * A copy of the MIT License is included in this file.
 *
 *
 * Terms of the MIT License:
 * ---------------------------------------------------
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
 * documentation files (the "Software"), to deal in the Software without restriction, including without limitation the
 * rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to
 * permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of
 * the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT
 * LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN
 * NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
 * SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package com.tencent.bkrepo.scanner.service.impl

import com.google.common.util.concurrent.UncheckedExecutionException
import com.tencent.bkrepo.auth.pojo.enums.PermissionAction
import com.tencent.bkrepo.common.api.exception.ErrorCodeException
import com.tencent.bkrepo.common.api.exception.NotFoundException
import com.tencent.bkrepo.common.api.message.CommonMessageCode
import com.tencent.bkrepo.common.api.pojo.Page
import com.tencent.bkrepo.common.api.util.toJsonString
import com.tencent.bkrepo.common.mongo.dao.util.Pages
import com.tencent.bkrepo.common.query.model.PageLimit
import com.tencent.bkrepo.common.scanner.pojo.scanner.SubScanTaskStatus
import com.tencent.bkrepo.common.security.permission.PrincipalType
import com.tencent.bkrepo.common.security.util.SecurityUtils
import com.tencent.bkrepo.repository.api.NodeClient
import com.tencent.bkrepo.repository.api.RepositoryClient
import com.tencent.bkrepo.scanner.component.ScannerPermissionCheckHandler
import com.tencent.bkrepo.scanner.component.manager.ScanExecutorResultManager
import com.tencent.bkrepo.scanner.component.manager.ScannerConverter
import com.tencent.bkrepo.scanner.dao.AbsSubScanTaskDao
import com.tencent.bkrepo.scanner.dao.ArchiveSubScanTaskDao
import com.tencent.bkrepo.scanner.dao.FileScanResultDao
import com.tencent.bkrepo.scanner.dao.PlanArtifactLatestSubScanTaskDao
import com.tencent.bkrepo.scanner.dao.ScanPlanDao
import com.tencent.bkrepo.scanner.dao.ScanTaskDao
import com.tencent.bkrepo.scanner.dao.SubScanTaskDao
import com.tencent.bkrepo.scanner.exception.ScanTaskNotFoundException
import com.tencent.bkrepo.scanner.model.LeakDetailExport
import com.tencent.bkrepo.scanner.model.LeakScanPlanExport
import com.tencent.bkrepo.scanner.model.TPlanArtifactLatestSubScanTask
import com.tencent.bkrepo.scanner.model.LicenseScanDetailExport
import com.tencent.bkrepo.scanner.model.LicenseScanPlanExport
import com.tencent.bkrepo.scanner.pojo.ScanTask
import com.tencent.bkrepo.scanner.pojo.request.ArtifactVulnerabilityRequest
import com.tencent.bkrepo.scanner.pojo.request.FileScanResultDetailRequest
import com.tencent.bkrepo.scanner.pojo.request.FileScanResultOverviewRequest
import com.tencent.bkrepo.scanner.pojo.request.ScanTaskQuery
import com.tencent.bkrepo.scanner.pojo.request.SubtaskInfoRequest
import com.tencent.bkrepo.scanner.pojo.request.scancodetoolkit.ArtifactLicensesDetailRequest
import com.tencent.bkrepo.scanner.pojo.response.SubtaskResultOverview
import com.tencent.bkrepo.scanner.pojo.response.ArtifactVulnerabilityInfo
import com.tencent.bkrepo.scanner.pojo.response.FileLicensesResultDetail
import com.tencent.bkrepo.scanner.pojo.response.FileLicensesResultOverview
import com.tencent.bkrepo.scanner.pojo.response.FileScanResultDetail
import com.tencent.bkrepo.scanner.pojo.response.FileScanResultOverview
import com.tencent.bkrepo.scanner.pojo.response.SubtaskInfo
import com.tencent.bkrepo.scanner.service.ScanTaskService
import com.tencent.bkrepo.scanner.service.ScannerService
import com.tencent.bkrepo.scanner.utils.Converter
import com.tencent.bkrepo.scanner.utils.EasyExcelUtils
import com.tencent.bkrepo.scanner.utils.ScanLicenseConverter
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.time.format.DateTimeFormatter

@Service
class ScanTaskServiceImpl(
    private val permissionCheckHandler: ScannerPermissionCheckHandler,
    private val scannerService: ScannerService,
    private val scanPlanDao: ScanPlanDao,
    private val scanTaskDao: ScanTaskDao,
    private val subScanTaskDao: SubScanTaskDao,
    private val archiveSubScanTaskDao: ArchiveSubScanTaskDao,
    private val planArtifactLatestSubScanTaskDao: PlanArtifactLatestSubScanTaskDao,
    private val fileScanResultDao: FileScanResultDao,
    private val nodeClient: NodeClient,
    private val repositoryClient: RepositoryClient,
    private val scanExecutorResultManagers: Map<String, ScanExecutorResultManager>,
    private val scannerConverters: Map<String, ScannerConverter>
) : ScanTaskService {

    private val logger = LoggerFactory.getLogger(javaClass)

    override fun task(taskId: String): ScanTask {
        return scanTaskDao.findById(taskId)?.let { task ->
            if (task.projectId == null) {
                permissionCheckHandler.permissionManager.checkPrincipal(SecurityUtils.getUserId(), PrincipalType.ADMIN)
            } else {
                permissionCheckHandler.checkProjectPermission(task.projectId, PermissionAction.MANAGE)
            }
            val plan = task.planId?.let { scanPlanDao.get(it) }
            Converter.convert(task, plan)
        } ?: throw ScanTaskNotFoundException(taskId)
    }

    override fun tasks(scanTaskQuery: ScanTaskQuery, pageLimit: PageLimit): Page<ScanTask> {
        permissionCheckHandler.checkProjectPermission(scanTaskQuery.projectId, PermissionAction.MANAGE)
        val taskPage = scanTaskDao.find(scanTaskQuery, pageLimit)
        val records = taskPage.records.map { Converter.convert(it) }
        return Page(pageLimit.pageNumber, pageLimit.pageSize, taskPage.totalRecords, records)
    }

    override fun subtaskOverview(subtaskId: String): SubtaskResultOverview {
        return subtaskOverview(subtaskId, archiveSubScanTaskDao)
    }

    override fun subtasks(request: SubtaskInfoRequest): Page<SubtaskInfo> {
        if (request.parentScanTaskId == null) {
            throw ErrorCodeException(CommonMessageCode.PARAMETER_INVALID)
        }
        return subtasks(request, archiveSubScanTaskDao)
    }

    override fun planArtifactSubtaskPage(request: SubtaskInfoRequest): Page<SubtaskInfo> {
        if (request.planId == null) {
            throw ErrorCodeException(CommonMessageCode.PARAMETER_INVALID)
        }
        return subtasks(request, planArtifactLatestSubScanTaskDao)
    }

    override fun exportScanPlanRecords(request: SubtaskInfoRequest): Map<String, Any> {
        val exportResultMap = mutableMapOf<String, Any>()
        with(request) {
            if (planId == null) {
                throw ErrorCodeException(CommonMessageCode.PARAMETER_INVALID)
            }
            // 方案信息，获取方案名称
            val scanPlan =
                scanPlanDao.find(projectId, planId!!) ?: throw ErrorCodeException(CommonMessageCode.PARAMETER_INVALID)
            exportResultMap["name"] = scanPlan.name

            // 获取任务信息
            // TODO 许可已实现，需要添加漏洞实现
            var pageNumber = 1
            var page = planArtifactLatestSubScanTaskDao.pageBy(request)
            val dataList = mutableListOf<LicenseScanPlanExport>()
            while (page.records.isNotEmpty()) {
                page.records.forEach {
                    dataList.add(ScanLicenseConverter.convert(it))
                }
                pageNumber++
                request.pageNumber = pageNumber
                page = planArtifactLatestSubScanTaskDao.pageBy(request)
            }
            exportResultMap["data"] = dataList
        }
        return exportResultMap
    }

    override fun planArtifactSubtaskOverview(subtaskId: String): SubtaskResultOverview {
        return subtaskOverview(subtaskId, planArtifactLatestSubScanTaskDao)
    }

    override fun resultOverview(request: FileScanResultOverviewRequest): List<FileScanResultOverview> {
        with(request) {
            val subScanTaskMap = subScanTaskDao
                .findByCredentialsKeyAndSha256List(credentialsKeyFiles)
                .associateBy { "${it.credentialsKey}:${it.sha256}" }

            return fileScanResultDao.findScannerResults(scanner, credentialsKeyFiles).map {
                val status = subScanTaskMap["${it.credentialsKey}:${it.sha256}"]?.status
                    ?: SubScanTaskStatus.SUCCESS.name
                // 只查询对应scanner的结果，此处必定不为null
                val scannerResult = it.scanResult[scanner]!!
                FileScanResultOverview(
                    status = status,
                    sha256 = it.sha256,
                    scanDate = scannerResult.startDateTime.format(DateTimeFormatter.ISO_DATE_TIME),
                    overview = scannerResult.overview
                )
            }
        }
    }

    override fun resultDetail(request: FileScanResultDetailRequest): FileScanResultDetail {
        with(request) {
            val node = artifactInfo!!.run {
                nodeClient.getNodeDetail(projectId, repoName, getArtifactFullPath())
            }.data!!
            val repo = repositoryClient.getRepoInfo(node.projectId, node.repoName).data!!

            val scanner = scannerService.get(scanner)
            val scanResultDetail = scanExecutorResultManagers[scanner.type]?.load(
                repo.storageCredentialsKey, node.sha256!!, scanner, arguments
            )
            val status = if (scanResultDetail == null) {
                subScanTaskDao.findByCredentialsAndSha256(repo.storageCredentialsKey, node.sha256!!)?.status
                    ?: SubScanTaskStatus.NEVER_SCANNED.name
            } else {
                SubScanTaskStatus.SUCCESS.name
            }
            return FileScanResultDetail(status, node.sha256!!, scanResultDetail)
        }
    }

    override fun resultDetail(request: ArtifactVulnerabilityRequest): Page<ArtifactVulnerabilityInfo> {
        return resultDetail(request, planArtifactLatestSubScanTaskDao)
    }

    override fun exportLeakDetail(request: ArtifactVulnerabilityRequest) {
        with(request) {
            val subtask = planArtifactLatestSubScanTaskDao.findById(subScanTaskId!!)
                ?: throw ErrorCodeException(CommonMessageCode.RESOURCE_NOT_FOUND, subScanTaskId!!)

            var resultDetailPage = resultDetail(request, planArtifactLatestSubScanTaskDao)
            var pageNumber = 1
            val resultList = mutableListOf<ArtifactVulnerabilityInfo>()
            while (resultDetailPage.records.isNotEmpty()) {
                resultList.addAll(resultDetailPage.records)
                resultDetailPage = resultDetail(
                    ArtifactVulnerabilityRequest(
                        projectId = projectId,
                        subScanTaskId = subScanTaskId,
                        pageNumber = ++pageNumber
                    )
                )
            }
            logger.info("resultList size:[${resultList.size}]")
            val dataList = mutableListOf<LeakDetailExport>()
            resultList.forEach {
                dataList.add(Converter.convertToDetailExport(it))
            }
            logger.info("export dataList:[${dataList.toJsonString()}]")
            if (dataList.isEmpty()) return
            EasyExcelUtils.download(dataList, subtask.artifactName, LeakDetailExport::class.java)
        }
    }

    override fun archiveSubtaskResultDetail(request: ArtifactVulnerabilityRequest): Page<ArtifactVulnerabilityInfo> {
        return resultDetail(request, archiveSubScanTaskDao)
    }

    private fun subtaskOverview(subtaskId: String, subtaskDao: AbsSubScanTaskDao<*>): SubtaskResultOverview {
        val subtask = subtaskDao.findById(subtaskId)
            ?: throw NotFoundException(CommonMessageCode.RESOURCE_NOT_FOUND, subtaskId)
        try {
            permissionCheckHandler.checkSubtaskPermission(subtask, PermissionAction.READ)
        } catch (e: UncheckedExecutionException) {
            logger.error("Failed to checkSubtaskPermission $e")
            permissionCheckHandler.checkProjectPermission(subtask.projectId, PermissionAction.MANAGE)
        }
        return Converter.convert(subtask)
    }

    private fun subtasks(request: SubtaskInfoRequest, subtaskDao: AbsSubScanTaskDao<*>): Page<SubtaskInfo> {
        with(request) {
            val page = subtaskDao.pageBy(request)
            return Pages.ofResponse(
                Pages.ofRequest(pageNumber, pageSize),
                page.totalRecords,
                page.records.map { Converter.convertToSubtaskInfo(it) }
            )
        }
    }

    override fun resultDetail(request: ArtifactLicensesDetailRequest): Page<FileLicensesResultDetail> {
        return resultDetail(request, planArtifactLatestSubScanTaskDao)
    }

    private fun resultDetail(
        request: ArtifactVulnerabilityRequest,
        subScanTaskDao: AbsSubScanTaskDao<*>
    ): Page<ArtifactVulnerabilityInfo> {
        with(request) {
            val subtask = subScanTaskDao.findById(subScanTaskId!!)
                ?: throw ErrorCodeException(CommonMessageCode.RESOURCE_NOT_FOUND, subScanTaskId!!)

            try {
                permissionCheckHandler.checkSubtaskPermission(subtask, PermissionAction.READ)
            } catch (e: UncheckedExecutionException) {
                logger.error("Failed to checkSubtaskPermission $e")
                permissionCheckHandler.checkProjectPermission(subtask.projectId, PermissionAction.MANAGE)
            }

            val scanner = scannerService.get(subtask.scanner)
            val scannerConverter = scannerConverters[ScannerConverter.name(scanner.type)]
            val arguments = scannerConverter?.convertToLoadArguments(request)
            val scanResultManager = scanExecutorResultManagers[subtask.scannerType]
            val detailReport = scanResultManager?.load(subtask.credentialsKey, subtask.sha256, scanner, arguments)

            return detailReport
                ?.let { scannerConverter?.convertCveResult(it) }
                ?: Pages.buildPage(emptyList(), pageSize, pageNumber)
        }
    }


    override fun planLicensesArtifact(projectId: String, subScanTaskId: String): FileLicensesResultOverview {
        return planLicensesArtifact(subScanTaskId, planArtifactLatestSubScanTaskDao)
    }

    override fun archiveSubtaskResultDetail(request: ArtifactLicensesDetailRequest): Page<FileLicensesResultDetail> {
        return resultDetail(request, archiveSubScanTaskDao)
    }

    override fun exportResultDetail(request: ArtifactLicensesDetailRequest) {
        val resultList = mutableListOf<FileLicensesResultDetail>()
        val subtask = planArtifactLatestSubScanTaskDao.findById(request.subScanTaskId!!)
            ?: throw ErrorCodeException(CommonMessageCode.RESOURCE_NOT_FOUND, request.subScanTaskId!!)

        var resultDetailPage = resultDetail(request, planArtifactLatestSubScanTaskDao)
        var pageNumber = 1
        while (resultDetailPage.records.isNotEmpty()) {
            resultList.addAll(resultDetailPage.records)
            resultDetailPage = resultDetail(
                ArtifactLicensesDetailRequest(
                    projectId = request.projectId,
                    subScanTaskId = request.subScanTaskId,
                    pageNumber = ++pageNumber
                )
            )
        }
        logger.info("resultList size:[${resultList.size}]")
        val resultListConvert = mutableListOf<LicenseScanDetailExport>()
        resultList.forEach {
            resultListConvert.add(ScanLicenseConverter.convert(it))
        }
        logger.info("resultListConvert:[${resultListConvert.toJsonString()}]")
        EasyExcelUtils.download(resultListConvert, subtask.artifactName, LicenseScanDetailExport::class.java)
    }

    override fun subtaskLicenseOverview(subtaskId: String): FileLicensesResultOverview {
        return planLicensesArtifact(subtaskId, archiveSubScanTaskDao)
    }


    private fun resultDetail(
        request: ArtifactLicensesDetailRequest,
        subScanTaskDao: AbsSubScanTaskDao<*>
    ): Page<FileLicensesResultDetail> {
        with(request) {
            val subtask = subScanTaskDao.findById(subScanTaskId!!)
                ?: throw ErrorCodeException(CommonMessageCode.RESOURCE_NOT_FOUND, subScanTaskId!!)
            try {
                permissionCheckHandler.checkSubtaskPermission(subtask, PermissionAction.READ)
            } catch (e: UncheckedExecutionException) {
                logger.error("Failed to checkSubtaskPermission $e")
                permissionCheckHandler.checkProjectPermission(subtask.projectId, PermissionAction.MANAGE)
            }
            val scanner = scannerService.get(subtask.scanner)
            val arguments = ScanLicenseConverter.convertToLoadArguments(request, scanner.type)
            val scanResultManager = scanExecutorResultManagers[subtask.scannerType]
            val detailReport = scanResultManager?.load(
                subtask.credentialsKey,
                subtask.sha256,
                scannerService.get(subtask.scanner),
                arguments
            )
            return ScanLicenseConverter.convert(detailReport, subtask.scannerType, reportType, pageNumber, pageSize)
        }
    }

    private fun planLicensesArtifact(
        subScanTaskId: String,
        subtaskDao: AbsSubScanTaskDao<*>
    ): FileLicensesResultOverview {
        val subtask = subtaskDao.findById(subScanTaskId)
            ?: throw NotFoundException(CommonMessageCode.RESOURCE_NOT_FOUND, subScanTaskId)
        try {
            permissionCheckHandler.checkSubtaskPermission(subtask, PermissionAction.READ)
        } catch (e: UncheckedExecutionException) {
            logger.error("Failed to checkSubtaskPermission $e")
            permissionCheckHandler.checkProjectPermission(subtask.projectId, PermissionAction.MANAGE)
        }
        return ScanLicenseConverter.convert(subtask)
    }
}
