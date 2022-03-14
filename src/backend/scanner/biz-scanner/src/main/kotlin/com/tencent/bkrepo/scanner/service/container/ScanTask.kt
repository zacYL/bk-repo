package com.tencent.bkrepo.scanner.service.container

import com.fasterxml.jackson.module.kotlin.readValue
import com.tencent.bkrepo.common.api.exception.NotFoundException
import com.tencent.bkrepo.common.api.message.CommonMessageCode
import com.tencent.bkrepo.common.api.util.JsonUtils
import com.tencent.bkrepo.common.api.util.toJsonString
import com.tencent.bkrepo.common.artifact.message.ArtifactMessageCode
import com.tencent.bkrepo.common.artifact.pojo.RepositoryType
import com.tencent.bkrepo.common.artifact.stream.Range
import com.tencent.bkrepo.common.storage.core.StorageService
import com.tencent.bkrepo.repository.api.NodeClient
import com.tencent.bkrepo.repository.api.PackageClient
import com.tencent.bkrepo.repository.api.RepositoryClient
import com.tencent.bkrepo.scanner.config.ExecutorConfig
import com.tencent.bkrepo.scanner.dao.ScanResultCveDao
import com.tencent.bkrepo.scanner.exception.LoadFileFailedException
import com.tencent.bkrepo.scanner.exception.RunContainerFailedException
import com.tencent.bkrepo.scanner.model.TPlanRecord
import com.tencent.bkrepo.scanner.model.TScanResultCve
import com.tencent.bkrepo.scanner.pojo.cve.CvesecItem
import com.tencent.bkrepo.scanner.pojo.enums.LeakType
import com.tencent.bkrepo.scanner.pojo.enums.ScanStatus
import com.tencent.bkrepo.scanner.util.TaskIdUtil
import org.apache.commons.io.FileUtils
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.mongodb.core.MongoTemplate
import org.springframework.data.mongodb.core.query.Criteria
import org.springframework.data.mongodb.core.query.Query
import org.springframework.data.mongodb.core.query.Update
import org.springframework.stereotype.Component
import java.io.File
import java.time.Duration
import java.time.LocalDateTime
import java.util.UUID
import java.util.concurrent.Executors

@Component
class ScanTask @Autowired constructor(
    private val mongoTemplate: MongoTemplate,
    private val scanResultCveDao: ScanResultCveDao,
    private val hostRunTime: HostRunTime,
    private val dockerRunTime: DockerRunTime,
    private val storageService: StorageService,
    private val nodeClient: NodeClient,
    private val packageClient: PackageClient,
    private var config: ExecutorConfig,
    private val repositoryClient: RepositoryClient
) {

    fun batchScan(scanRecordList: List<TPlanRecord>): Boolean {
        logger.info("batch scan record list:${scanRecordList.toJsonString()}")
        val taskId = TaskIdUtil.build()
        scanRecordList.forEach {
            val task = {
                val workDir = getWorkDir(taskId)
                val outputDir = getOutputDir(workDir)
                logger.info("workDir:$workDir, outputDir:$outputDir")
                scanFile(
                    planRecord = it,
                    workDir = workDir,
                    outputDir = outputDir,
                    taskId = taskId
                )
                logger.info("finish scan file [$it] ")
            }
            executor.submit(task)
        }
        return true
    }

    /**
     * 自动扫描：更新一个制品，对应多个扫描方案，只扫描一次，扫描结果更新到所有方案
     */
    fun atomScan(scanRecordSet: List<TPlanRecord>): Boolean {
        logger.info("atom scan record list:${scanRecordSet.toJsonString()}")
        val taskId = TaskIdUtil.build()
        val task = {
            val workDir = getWorkDir(taskId)
            val outputDir = getOutputDir(workDir)
            logger.info("workDir:$workDir, outputDir:$outputDir")
            atomScanFile(
                scanRecordList = scanRecordSet,
                workDir = workDir,
                outputDir = outputDir,
                taskId = taskId
            )
            logger.info("finish atom scan [$scanRecordSet] ")
        }
        executor.submit(task)
        return true
    }

    fun containerScan(
        workDir: String,
        taskId: String,
        planRecord: TPlanRecord,
        recordIdList: List<String>
    ): Boolean {
        // 生成运行时环境
        hostRunTime.buildWorkSpace(workDir)

        // 加载待扫描文件
        val sha256 = downloadFile(planRecord, workDir) ?: run {
            logger.warn("load file fail [$planRecord]")
            throw LoadFileFailedException("load file failed")
        }

        // 生成配置文件
        hostRunTime.loadConfigFile(taskId, workDir, config, sha256)

        //已停止扫描，不执行扫描任务
        if (scanStop(planRecord.projectId, planRecord.id!!)) return false

        //创建容器
        val containerId = dockerRunTime.createContainer(workDir)
        planRecord.containerId = containerId
        //自动扫描时匹配到多个方案，每条扫描记录都需要保存容器id
        saveContainerId(
            projectId = planRecord.projectId,
            recordIdList = recordIdList,
            containerId = containerId,
            updateUser = planRecord.createdBy
        )
        // 执行任务
        dockerRunTime.runContainerOnce(workDir, containerId)
        return true
    }

    /**
     * 批量更新容器id
     */
    private fun saveContainerId(
        projectId: String,
        recordIdList: List<String>,
        containerId: String,
        updateUser: String
    ) {
        logger.info("save ContainerId, recordIdList:${recordIdList}")
        val criteria = batchRecordCriteria(projectId, recordIdList)
        val query = Query(criteria)
        val update = Update()
            .set(TPlanRecord::containerId.name, containerId)
            .set(TPlanRecord::lastModifiedBy.name, updateUser)
            .set(TPlanRecord::lastModifiedDate.name, LocalDateTime.now())
        val result = mongoTemplate.updateMulti(query, update, TPlanRecord::class.java)
        logger.info("saveContainerId result:${result.toJsonString()}")
    }

    private fun scanStop(projectId: String, recordId: String): Boolean {
        val query = Query.query(
            Criteria.where(TPlanRecord::projectId.name).`is`(projectId)
                .and(TPlanRecord::id.name).`is`(recordId)
                .and(TPlanRecord::scanStatus.name).`is`(ScanStatus.STOP.name)
                .and(TPlanRecord::delete.name).`is`(false)
        )
        val findOne = mongoTemplate.findOne(query, TPlanRecord::class.java) ?: return false
        logger.info("findOne:${findOne.toJsonString()}")
        return true
    }

    fun atomScanFile(
        scanRecordList: List<TPlanRecord>,
        workDir: String,
        outputDir: String,
        taskId: String
    ) {
        logger.info("start to atomScanFile")
        //取一条记录出来扫描
        val record = scanRecordList.first()
        val recordIdList = scanRecordList.map { it.id!! }
        //更新所有任务状态
        setPlanRecordStatus(record, ScanStatus.RUNNING.toString(), recordIdList)
        try {
            val scanFinish = containerScan(workDir, taskId, record, recordIdList)
            //完成扫描且未停止扫描，保存扫描报告
            if (scanFinish) {
                // 输出报告
                scanRecordList.forEach { planRecord ->
                    if (!scanStop(record.projectId, planRecord.id!!)) {
                        reportOutput(planRecord, outputDir)
                    }
                }
            }
        } catch (e: RuntimeException) {
            logger.error("scan file exception [${scanRecordList.first()}, $e]")
            setPlanRecordStatus(record, ScanStatus.FAILED.toString(), recordIdList)
        } finally {
            // 清理工作目录
            if (config.clean) {
                logger.info("cleanWorkSpace...")
                hostRunTime.cleanWorkSpace(workDir)
            }
        }
    }

    private fun scanFile(
        planRecord: TPlanRecord,
        workDir: String,
        outputDir: String,
        taskId: String
    ) {
        logger.info("start to run file [$planRecord]")
        val idList = listOf(planRecord.id!!)
        //修改扫描状态
        setPlanRecordStatus(planRecord, ScanStatus.RUNNING.toString(), idList)
        try {
            //容器扫描
            val scanFinish = containerScan(workDir, taskId, planRecord, idList)

            //完成扫描且未停止扫描，保存扫描报告
            if (scanFinish && !scanStop(planRecord.projectId, planRecord.id!!)) {
                // 输出报告
                reportOutput(planRecord, outputDir)
            }
        } catch (e: RunContainerFailedException) {
            logger.error("scan file exception [$planRecord, $e]")
            setPlanRecordStatus(planRecord, ScanStatus.FAILED.toString(), idList)
        } finally {
            // 清理工作目录
            if (config.clean) {
                logger.info("cleanWorkSpace...")
                hostRunTime.cleanWorkSpace(workDir)
            }
        }
    }

    /**
     * 产出漏洞报告分析
     */
    private fun reportOutput(planRecord: TPlanRecord, outputDir: String) {
        val file = File("$outputDir$CVE_REPORT")
        logger.info("scanResult file:${file.absolutePath}")
        if (!file.exists()) {
            logger.info("No vulnerability reports were scanned")
            //更新扫描耗时，扫描状态
            updatePlanRecord(planRecord, null, ScanStatus.SUCCESS)
            return
        }
        logger.info("save report[${file.absolutePath}] to db")
        val content = file.readText()
        //解析报告，写入扫描结果
        val cveItems = JsonUtils.objectMapper.readValue<ArrayList<CvesecItem>>(content)
        logger.info("cveItems:${cveItems.toJsonString()}")
        //漏洞列表
        val cveList = mutableListOf<TScanResultCve>()
        var critical = 0
        var high = 0
        var medium = 0
        var low = 0
        val cveIdList = mutableListOf<String>()
        //解析漏洞报告
        cveItems.forEach { cveItem ->
            val cveInfo = cveItem.nvtoolsCveinfo
            val cveId = cveInfo.cve_id
            // 漏洞去重
            if (cveIdList.contains(cveId)) return@forEach
            cveIdList.add(cveId)
            val referenceList = cveInfo.reference?.let {
                JsonUtils.objectMapper.readValue<ArrayList<String>>(it)
            }

            val severity = when (cveInfo.level) {
                LeakType.CRITICAL.value -> {
                    critical += 1
                    LeakType.CRITICAL.name
                }
                LeakType.HIGH.value -> {
                    high += 1
                    LeakType.HIGH.name
                }
                LeakType.MEDIUM.value -> {
                    medium += 1
                    LeakType.MEDIUM.name
                }
                LeakType.LOW.value -> {
                    low += 1
                    LeakType.LOW.name
                }
                else -> throw NotFoundException(CommonMessageCode.RESOURCE_NOT_FOUND, cveInfo.level)
            }
            cveList.add(
                TScanResultCve(
                    id = UUID.randomUUID().toString(),
                    createdBy = planRecord.createdBy,
                    createdDate = LocalDateTime.now(),
                    lastModifiedBy = planRecord.createdBy,
                    lastModifiedDate = LocalDateTime.now(),
                    recordId = planRecord.id!!,
                    cveId = cveId,
                    severity = severity,
                    pkgName = cveItem.libName,
                    installedVersion = cveItem.version,
                    title = cveInfo.name,
                    description = cveInfo.des,
                    officialSolution = cveInfo.defense_solution,
                    reference = referenceList
                )
            )
        }

        //保存扫描记录与漏洞数据
        val highestLeakLevel = when {
            critical > 0 -> LeakType.CRITICAL.name
            high > 0 -> LeakType.HIGH.name
            medium > 0 -> LeakType.MEDIUM.name
            low > 0 -> LeakType.LOW.name
            else -> throw NotFoundException(CommonMessageCode.RESOURCE_NOT_FOUND,
                "Can not find LeakType")
        }
        planRecord.highestLeakLevel = highestLeakLevel
        planRecord.critical = critical
        planRecord.high = high
        planRecord.medium = medium
        planRecord.low = low

        //漏洞信息入库
        val result = updatePlanRecord(planRecord, content, ScanStatus.SUCCESS)
        //保存漏洞明细cveList，planRecord中止不插入漏洞数据
        if (result) {
            logger.info("insert cve:${cveList.toJsonString()}")
            scanResultCveDao.insert(cveList)
        }
    }

    fun updatePlanRecord(planRecord: TPlanRecord, content: String?, status: ScanStatus): Boolean {
        with(planRecord) {
            val criteria = Criteria.where(TPlanRecord::projectId.name).`is`(projectId)
                .and(TPlanRecord::id.name).`is`(id!!)
                .and(TPlanRecord::delete.name).`is`(false)
                .and(TPlanRecord::scanStatus.name).ne(ScanStatus.STOP.name)
            val query = Query(criteria)

            val update = Update()
                .set(TPlanRecord::critical.name, critical)
                .set(TPlanRecord::high.name, high)
                .set(TPlanRecord::medium.name, medium)
                .set(TPlanRecord::low.name, low)
                .set(TPlanRecord::scanStatus.name, status.name)
                //扫描持续时长，当前时间-最后一次修改时间
                .set(TPlanRecord::duration.name, Duration.between(lastModifiedDate, LocalDateTime.now()).toMillis())
                .set(TPlanRecord::lastModifiedBy.name, createdBy)
                .set(TPlanRecord::lastModifiedDate.name, LocalDateTime.now())
                .set(TPlanRecord::cveContent.name, content)
                .set(TPlanRecord::highestLeakLevel.name, highestLeakLevel)
            val updateRecord = mongoTemplate.updateFirst(query, update, TPlanRecord::class.java)
            logger.info("updateRecord:${updateRecord.toJsonString()}")
            return updateRecord.modifiedCount == 1L || updateRecord.matchedCount == 1L
        }
    }

    /**
     * 加载待扫描文件
     */
    private fun downloadFile(planRecord: TPlanRecord, workDir: String): String? {
        with(planRecord) {
            try {
                val repository = repositoryClient.getRepoDetail(projectId, repoName).data
                logger.info("repository:$repository")
                if (repository == null) {
                    logger.warn("fail to get the repo [$planRecord]")
                    return null
                }

                //maven仓库，查找fullPath
                if (repoType == RepositoryType.MAVEN.toString()) {
                    val packageVersion = packageClient.findVersionByName(
                        projectId = projectId,
                        repoName = repoName,
                        packageKey = packageKey!!,
                        version = version!!
                    ).data ?: throw NotFoundException(ArtifactMessageCode.VERSION_NOT_FOUND, version)
                    logger.info("packageVersion:$packageVersion")
                    fullPath = packageVersion.contentPath!!
                }

                logger.info("fullPath:$fullPath")
                val node = nodeClient.getNodeDetail(projectId, repoName, fullPath!!).data
                logger.info("get node:$node")
                if (node == null) {
                    logger.warn("fail to get the node [$planRecord]")
                    return null
                }
                val path = "$workDir${config.inputDir}${node.sha256}"
                val file = File(path)
                logger.info("file:${file.absolutePath}")
                val inputStream = storageService.load(
                    node.sha256!!, Range.full(node.size),
                    repository.storageCredentials
                )
                logger.info("inputStream:$inputStream")
                inputStream.use {
                    FileUtils.copyInputStreamToFile(inputStream, file)
                }
                return node.sha256
            } catch (e: Exception) {
                logger.warn("load file to runtime exception [$e] ")
                return null
            }
        }
    }

    fun batchRecordCriteria(projectId: String, recordIdList: List<String>): Criteria {
        return Criteria.where(TPlanRecord::projectId.name).`is`(projectId)
            .and(TPlanRecord::id.name).`in`(recordIdList)
            .and(TPlanRecord::delete.name).`is`(false)
            .and(TPlanRecord::scanStatus.name).ne(ScanStatus.STOP.name)
    }

    /**
     * 批量更新扫描记录状态
     */
    private fun setPlanRecordStatus(
        planRecord: TPlanRecord,
        scanStatus: String,
        recordIdList: List<String>
    ): Boolean {
        with(planRecord) {
            val criteria = batchRecordCriteria(projectId, recordIdList)
            val query = Query()
            query.addCriteria(criteria)
            val update = Update()
            update.set(TPlanRecord::scanStatus.name, scanStatus)
            update.set(TPlanRecord::lastModifiedDate.name, LocalDateTime.now())
            update.set(TPlanRecord::lastModifiedBy.name, createdBy)
            logger.info("criteria:${criteria.toJsonString()}, query:$query, update:$update")
            val result = mongoTemplate.updateMulti(query, update, TPlanRecord::class.java)
            logger.info("update status result:${result.toJsonString()}, recordIdList.size:${recordIdList.size}")
            return result.modifiedCount == recordIdList.size.toLong()
        }
    }

    fun getWorkDir(taskId: String): String {
        return "${config.rootDir}/$taskId"
    }

    fun getOutputDir(workDir: String): String {
        return "$workDir${config.outputDir}"
    }

    companion object {

        private val logger = LoggerFactory.getLogger(ScanTask::class.java)

        private const val CVE_REPORT = "cvesec_items.json"

        private val executor = Executors.newSingleThreadExecutor()
    }

}
