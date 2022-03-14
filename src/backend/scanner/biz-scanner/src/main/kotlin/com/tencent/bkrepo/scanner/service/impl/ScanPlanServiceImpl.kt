package com.tencent.bkrepo.scanner.service.impl

import com.tencent.bkrepo.common.api.exception.ErrorCodeException
import com.tencent.bkrepo.common.api.exception.NotFoundException
import com.tencent.bkrepo.common.api.message.CommonMessageCode
import com.tencent.bkrepo.common.api.pojo.Page
import com.tencent.bkrepo.common.api.util.toJsonString
import com.tencent.bkrepo.common.artifact.pojo.RepositoryType
import com.tencent.bkrepo.common.checker.message.ScanMessageCode
import com.tencent.bkrepo.common.mongo.dao.util.Pages
import com.tencent.bkrepo.scanner.dao.PlanRecordDao
import com.tencent.bkrepo.scanner.dao.ScanPlanDao
import com.tencent.bkrepo.scanner.model.TPlanRecord
import com.tencent.bkrepo.scanner.model.TScanPlan
import com.tencent.bkrepo.scanner.pojo.ArtifactRelationPlan
import com.tencent.bkrepo.scanner.pojo.ScanArtifactInfo
import com.tencent.bkrepo.scanner.pojo.ScanPlanBase
import com.tencent.bkrepo.scanner.pojo.ScanPlanInfo
import com.tencent.bkrepo.scanner.pojo.context.ArtifactPlanContext
import com.tencent.bkrepo.scanner.pojo.context.PlanArtifactContext
import com.tencent.bkrepo.scanner.pojo.enums.PlanType
import com.tencent.bkrepo.scanner.pojo.enums.ScanStatus
import com.tencent.bkrepo.scanner.pojo.request.ScanPlanRequest
import com.tencent.bkrepo.scanner.service.ScanPlanService
import com.tencent.bkrepo.scanner.util.ScanParamUtil
import com.tencent.bkrepo.scanner.util.TimeFormatUtil
import org.slf4j.LoggerFactory
import org.springframework.data.domain.Sort
import org.springframework.data.mongodb.core.MongoTemplate
import org.springframework.data.mongodb.core.query.Criteria
import org.springframework.data.mongodb.core.query.Query
import org.springframework.data.mongodb.core.query.Update
import org.springframework.stereotype.Service
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.UUID

@Service
class ScanPlanServiceImpl(
    private val scanPlanDao: ScanPlanDao,
    private val planRecordDao: PlanRecordDao,
    private val mongoTemplate: MongoTemplate
) : ScanPlanService {

    override fun createScanPlan(userId: String, request: ScanPlanRequest): Boolean {
        logger.info("userId:$userId, createScanPlan request:[$request]")
        with(request) {
            if (name.isNullOrEmpty() || name.length > PLAN_NAME_LENGTH_MAX) {
                throw ErrorCodeException(CommonMessageCode.PARAMETER_INVALID, "name cannot be empty or length > 32")
            }
            scanPlanDao.getPlan(projectId, name, null)?.let {
                logger.error("scan plan [$name] is exist.")
                throw ErrorCodeException(CommonMessageCode.RESOURCE_EXISTED, name)
            }
            val tScanPlan = TScanPlan(
                id = UUID.randomUUID().toString(),
                projectId = projectId,
                name = name,
                type = type ?: throw ErrorCodeException(
                    CommonMessageCode.PARAMETER_INVALID,
                    "ScanPlan type cannot be empty"
                ),
                description = description,
                severities = null,
                whitelists = null,
                autoScan = autoScan ?: false,
                repoNameList = repoNameList ?: emptyList(),
                artifactRules = artifactRules ?: emptyList(),
                createdBy = userId,
                createdDate = LocalDateTime.now(),
                lastModifiedBy = userId,
                lastModifiedDate = LocalDateTime.now()
            )
            logger.info("insert tScanPlan:$tScanPlan")
            scanPlanDao.insert(tScanPlan)
        }
        return true
    }

    override fun scanPlanList(projectId: String, type: PlanType?): List<ScanPlanBase> {
        val criteria = Criteria.where(TScanPlan::projectId.name).`is`(projectId)
            .and(TScanPlan::delete.name).`is`(false)
        type?.let { criteria.and(TScanPlan::type.name).`is`(type.name) }
        logger.info("criteria:${criteria.toJsonString()}")
        val query = Query(criteria).with(Sort.by(TScanPlan::createdDate.name).descending())
        val recordList = mongoTemplate.find(query, TScanPlan::class.java)
        logger.info("recordList:$recordList")

        return recordList.map { convert2PlanBase(it) }
    }

    override fun scanPlanList(
        projectId: String,
        type: PlanType?,
        name: String?,
        pageNumber: Int,
        pageSize: Int
    ): Page<ScanPlanInfo?> {
        val pageRequest = Pages.ofRequest(pageNumber, pageSize)
        val query = buildScanPlanPageQuery(projectId, type, name)
        logger.info("query:$query")
        val totalRecords = scanPlanDao.count(query)
        val records = scanPlanDao.find(query.with(pageRequest)).map { convert(it) }
        return Pages.ofResponse(pageRequest, totalRecords, records)
    }

    override fun countInfo(projectId: String, id: String): ScanPlanInfo? {
        return scanPlanDao.getPlan(projectId, null, id)?.let { convert(it) }
    }

    override fun getScanPlanBase(projectId: String, id: String): ScanPlanBase? {
        return scanPlanDao.getPlan(projectId, null, id)?.let {
            convert2PlanBase(it)
        }
    }

    private fun convert2PlanBase(scanPlan: TScanPlan): ScanPlanBase {
        return scanPlan.let {
            ScanPlanBase(
                id = it.id!!,
                name = it.name,
                type = it.type.name,
                description = it.description,
                projectId = it.projectId,
                autoScan = it.autoScan,
                repoNameList = it.repoNameList,
                artifactRules = it.artifactRules,
                createdBy = it.createdBy,
                createdDate = it.createdDate.format(DateTimeFormatter.ISO_DATE_TIME),
                lastModifiedBy = it.lastModifiedBy,
                lastModifiedDate = it.lastModifiedDate.format(DateTimeFormatter.ISO_DATE_TIME)
            )
        }
    }

    override fun updateStatus(userId: String, projectId: String, id: String): Boolean {
        logger.info("deleteScanPlan userId:$userId, projectId:$projectId, planId:$id")
        //方案正在使用，不能删除
        if (planRunCheck(projectId, id))
            throw ErrorCodeException(ScanMessageCode.RUNNING_PLAN_DEL)

        val criteria = Criteria.where(TScanPlan::projectId.name).`is`(projectId)
            .and(TScanPlan::id.name).`is`(id)
        val query = Query(criteria)
        val update = Update().set(TScanPlan::delete.name, true)
            .set(TScanPlan::lastModifiedBy.name, userId)
            .set(TScanPlan::lastModifiedDate.name, LocalDateTime.now())
        logger.info("criteria:${criteria.toJsonString()}, query:$query, update:$update")
        val result = mongoTemplate.updateFirst(query, update, TScanPlan::class.java)
        return result.modifiedCount == 1L
    }

    private fun planRunCheck(projectId: String, id: String): Boolean {
        getPlanRecord(projectId, id).forEach {
            when (it.scanStatus) {
                ScanStatus.INIT.name -> return true
                ScanStatus.RUNNING.name -> return true
            }
        }
        return false
    }

    override fun artifactPlanStatus(paramContext: ArtifactPlanContext): String? {
        logger.info("paramContext:${paramContext.toJsonString()}")
        var init = 0
        var finish = 0
        val planList = artifactPlanList(paramContext)
        if (planList.isNullOrEmpty()) return null
        logger.info("planList:${planList.toJsonString()}")
        planList.forEach {
            when (it.status) {
                ScanStatus.RUNNING.name -> return it.status
                ScanStatus.INIT.name -> init += 1
                else -> finish += 1 // stop/success/failed都是完成状态
            }
        }
        logger.info("init:$init, finish:$finish, planList.size:${planList.size}")
        return if (init > 0) ScanStatus.INIT.name else ScanStatus.SUCCESS.name
    }

    override fun artifactPlanList(paramContext: ArtifactPlanContext): List<ArtifactRelationPlan>? {
        logger.info("paramContext:${paramContext.toJsonString()}")
        with(paramContext) {
            ScanParamUtil.checkParam(
                repoType = repoType,
                artifactName = fullPath ?: "",
                packageKey = packageKey,
                version = version,
                fullPath = fullPath
            )
            //多个方案扫描过相同项目-仓库-同一个制品
            val criteria = Criteria.where(TPlanRecord::projectId.name).`is`(projectId)
                .and(TPlanRecord::repoName.name).`is`(repoName)
                .and(TPlanRecord::repoType.name).`is`(repoType.name)
                .and(TPlanRecord::delete.name).`is`(false)
            when (repoType) {
                RepositoryType.GENERIC -> {
                    criteria.and(TPlanRecord::fullPath.name).`is`(fullPath)
                }
                RepositoryType.MAVEN -> {
                    criteria.and(TPlanRecord::packageKey.name).`is`(packageKey)
                    criteria.and(TPlanRecord::version.name).`is`(version)
                }
                else -> throw ErrorCodeException(CommonMessageCode.PARAMETER_INVALID, repoType.name)
            }
            val query = Query.query(criteria)
            logger.info("artifactPlanList query:$query")
            //一条记录对应一个方案
            val recordList = mongoTemplate.find(query, TPlanRecord::class.java)
            logger.info("recordList:$recordList")
            //制品关联的方案
            return recordList.map {
                val plan = scanPlanDao.getPlan(projectId, null, it.planId)
                ArtifactRelationPlan(
                    projectId = projectId,
                    id = it.planId,
                    planType = plan!!.type.name,
                    name = plan.name,
                    status = it.scanStatus,
                    recordId = it.id!!
                )
            }
        }
    }

    override fun planArtifactList(paramContext: PlanArtifactContext): Page<ScanArtifactInfo?> {
        logger.info("paramContext:${paramContext.toJsonString()}")
        val pageRequest = Pages.ofRequest(paramContext.pageNumber, paramContext.pageSize)
        paramContext.status?.let {
            //有完成扫描时间只能查扫描成功的记录
            if (!paramContext.startTime.isNullOrEmpty() &&
                !paramContext.endTime.isNullOrEmpty() &&
                paramContext.status != ScanStatus.SUCCESS.name
            ) {
                return Pages.ofResponse(pageRequest, 0, emptyList())
            }
        }
        val query = buildPlanRecordPageQuery(paramContext)
        logger.info("query:$query")
        val totalRecords = planRecordDao.count(query)
        val records = planRecordDao.find(query.with(pageRequest)).map {
            artifactInfo(it)
        }
        return Pages.ofResponse(pageRequest, totalRecords, records)
    }

    override fun updateScanPlan(userId: String, request: ScanPlanRequest): Boolean {
        logger.info("userId:$userId, updateScanPlan:[$request]")
        with(request) {
            if (id.isNullOrEmpty()) {
                throw ErrorCodeException(CommonMessageCode.PARAMETER_INVALID, "planId is empty")
            }
            scanPlanDao.getPlan(projectId, null, id) ?: run {
                throw NotFoundException(CommonMessageCode.RESOURCE_NOT_FOUND, request.toString())
            }

            /*//方案正在扫描，不修改
            if (planRunningCheck(projectId, id))
                throw UnsupportedOperationException("plan($id) is running, cannot be update")*/

            val criteria = Criteria.where(TScanPlan::projectId.name).`is`(projectId)
                .and(TScanPlan::id.name).`is`(id)
            val update = Update()
            name?.let {
                update.set(TScanPlan::name.name, name)
            }
            description?.let {
                update.set(TScanPlan::description.name, description)
            }
            autoScan?.let {
                update.set(TScanPlan::autoScan.name, autoScan)
            }
            repoNameList?.let {
                update.set(TScanPlan::repoNameList.name, repoNameList)
            }
            artifactRules?.let {
                update.set(TScanPlan::artifactRules.name, artifactRules)
            }
            update.set(TScanPlan::lastModifiedBy.name, userId)
            update.set(TScanPlan::lastModifiedDate.name, LocalDateTime.now())
            val query = Query()
            query.addCriteria(criteria)

            logger.info("criteria:${criteria.toJsonString()}, query:$query, update:$update")
            val result = mongoTemplate.updateFirst(query, update, TScanPlan::class.java)
            return result.modifiedCount == 1L
        }
    }

    private fun artifactInfo(planRecord: TPlanRecord?): ScanArtifactInfo? {
        return planRecord?.let {
            val finishTime = if (it.scanStatus == ScanStatus.SUCCESS.name) {
                it.lastModifiedDate
            } else {
                null
            }
            ScanArtifactInfo(
                recordId = it.id!!,
                name = it.artifactName,
                packageKey = it.packageKey,
                version = it.version,
                fullPath = it.fullPath,
                repoType = it.repoType,
                repoName = it.repoName,
                highestLeakLevel = it.highestLeakLevel,
                duration = it.duration,
                finishTime = finishTime,
                status = it.scanStatus,
                createdBy = it.createdBy,
                createdDate = it.createdDate
            )
        }
    }

    /**
     * 获取方案扫描记录
     */
    fun getPlanRecord(projectId: String, planId: String): List<TPlanRecord> {
        val query = Query.query(
            Criteria.where(TPlanRecord::projectId.name).`is`(projectId)
                .and(TPlanRecord::planId.name).`is`(planId)
                .and(TPlanRecord::delete.name).`is`(false)
        ).with(Sort.by(TPlanRecord::createdDate.name).descending())
        logger.info("scanPlan recordList query:$query")
        return mongoTemplate.find(query, TPlanRecord::class.java)
    }

    private fun convert(scanPlan: TScanPlan?): ScanPlanInfo? {
        return scanPlan?.let {
            val recordList = getPlanRecord(it.projectId, it.id!!)
            logger.info("recordList:$recordList")
            val lastScanDate = recordList.firstOrNull()?.createdDate
            //数据统计
            val artifactCount = recordList.size
            var critical = 0
            var high = 0
            var medium = 0
            var low = 0

            val planStatus = getPlanStatus(recordList)
            recordList.forEach { record ->
                critical += record.critical
                high += record.high
                medium += record.medium
                low += record.low
            }

            ScanPlanInfo(
                projectId = it.projectId,
                id = it.id!!,
                planType = it.type.name,
                name = it.name,
                status = planStatus,
                artifactCount = artifactCount,
                critical = critical,
                high = high,
                medium = medium,
                low = low,
                total = critical + high + medium + low,
                createdBy = it.createdBy,
                createdDate = it.createdDate.format(DateTimeFormatter.ISO_DATE_TIME),
                lastModifiedBy = it.lastModifiedBy,
                lastModifiedDate = it.lastModifiedDate.format(DateTimeFormatter.ISO_DATE_TIME),
                lastScanDate = lastScanDate?.format(DateTimeFormatter.ISO_DATE_TIME)
            )
        }
    }

    private fun getPlanStatus(recordList: List<TPlanRecord>): String {
        if (recordList.isEmpty()) {
            return ScanStatus.INIT.name
        }
        var init = 0
        var finish = 0
        recordList.forEach { record ->
            when (record.scanStatus) {
                ScanStatus.RUNNING.name -> return record.scanStatus
                ScanStatus.INIT.name -> init += 1
                else -> finish += 1
            }
        }
        logger.info("init:$init, finish:$finish")
        return if (init > 0) ScanStatus.INIT.name else ScanStatus.SUCCESS.name
    }

    private fun buildScanPlanPageQuery(
        projectId: String,
        type: PlanType?,
        name: String?
    ): Query {
        val criteria = Criteria.where(TScanPlan::projectId.name).`is`(projectId)
            .and(TScanPlan::delete.name).`is`(false)
        type?.let { criteria.and(TScanPlan::type.name).`is`(type.name) }
        name?.let {
            criteria.and(TScanPlan::name.name).regex(".*$name.*")
        }
        return Query(criteria).with(Sort.by(TScanPlan::createdDate.name).descending())
    }

    private fun buildPlanRecordPageQuery(paramContext: PlanArtifactContext): Query {
        with(paramContext) {
            val criteria = Criteria.where(TPlanRecord::projectId.name).`is`(projectId)
                .and(TPlanRecord::planId.name).`is`(planId)
                .and(TPlanRecord::delete.name).`is`(false)
            artifactName?.let {
                criteria.and(TPlanRecord::artifactName.name).regex(".*$artifactName.*")
            }
            highestLeakLevel?.let {
                criteria.and(TPlanRecord::highestLeakLevel.name).`is`(highestLeakLevel)
            }
            repoType?.let { criteria.and(TPlanRecord::repoType.name).`is`(repoType) }
            repoName?.let { criteria.and(TPlanRecord::repoName.name).`is`(repoName) }
            if (startTime != null && endTime != null) {
                val localStart = TimeFormatUtil.convertToLocalTime(startTime)
                val localEnd = TimeFormatUtil.convertToLocalTime(endTime)
                criteria.and(TPlanRecord::lastModifiedDate.name).gte(localStart).lte(localEnd)
                criteria.and(TPlanRecord::scanStatus.name).`is`(ScanStatus.SUCCESS.name)
            } else if (startTime != null && endTime == null) {
                val localStart = TimeFormatUtil.convertToLocalTime(startTime)
                criteria.and(TPlanRecord::lastModifiedDate.name).gte(localStart)
                criteria.and(TPlanRecord::scanStatus.name).`is`(ScanStatus.SUCCESS.name)
            } else if (startTime == null && endTime != null) {
                val localEnd = TimeFormatUtil.convertToLocalTime(endTime)
                criteria.and(TPlanRecord::lastModifiedDate.name).lte(localEnd)
                criteria.and(TPlanRecord::scanStatus.name).`is`(ScanStatus.SUCCESS.name)
            } else {
                status?.let { criteria.and(TPlanRecord::scanStatus.name).`is`(status) }
            }
            return Query(criteria).with(
                Sort.by(Sort.Direction.DESC, TPlanRecord::createdDate.name, TPlanRecord::repoName.name,
                    TPlanRecord::fullPath.name, TPlanRecord::packageKey.name, TPlanRecord::version.name)
            )
        }
    }

    companion object {

        private val logger = LoggerFactory.getLogger(ScanPlanServiceImpl::class.java)
        private const val PLAN_NAME_LENGTH_MAX = 32
        private val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSz")

    }
}
