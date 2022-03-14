package com.tencent.bkrepo.scanner.service.impl

import com.tencent.bkrepo.common.api.exception.MethodNotAllowedException
import com.tencent.bkrepo.common.api.exception.NotFoundException
import com.tencent.bkrepo.common.api.message.CommonMessageCode
import com.tencent.bkrepo.common.api.pojo.Page
import com.tencent.bkrepo.common.api.util.toJsonString
import com.tencent.bkrepo.common.artifact.exception.NodeNotFoundException
import com.tencent.bkrepo.common.artifact.exception.PackageNotFoundException
import com.tencent.bkrepo.common.artifact.exception.RepoNotFoundException
import com.tencent.bkrepo.common.artifact.message.ArtifactMessageCode
import com.tencent.bkrepo.common.artifact.pojo.RepositoryType
import com.tencent.bkrepo.common.mongo.dao.util.Pages
import com.tencent.bkrepo.repository.api.NodeClient
import com.tencent.bkrepo.repository.api.PackageClient
import com.tencent.bkrepo.repository.api.RepositoryClient
import com.tencent.bkrepo.repository.pojo.node.NodeInfo
import com.tencent.bkrepo.repository.pojo.node.NodeListOption
import com.tencent.bkrepo.repository.pojo.packages.PackageListOption
import com.tencent.bkrepo.repository.pojo.packages.PackageSummary
import com.tencent.bkrepo.scanner.dao.PlanRecordDao
import com.tencent.bkrepo.scanner.dao.ScanPlanDao
import com.tencent.bkrepo.scanner.dao.ScanResultCveDao
import com.tencent.bkrepo.scanner.exception.ArtifactNotMatchException
import com.tencent.bkrepo.scanner.exception.PlanNotFoundException
import com.tencent.bkrepo.scanner.exception.RepeatScanException
import com.tencent.bkrepo.scanner.model.TPlanRecord
import com.tencent.bkrepo.scanner.model.TScanPlan
import com.tencent.bkrepo.scanner.model.TScanResultCve
import com.tencent.bkrepo.scanner.pojo.ArtifactCountInfo
import com.tencent.bkrepo.scanner.pojo.ArtifactLeakInfo
import com.tencent.bkrepo.scanner.pojo.enums.LeakType
import com.tencent.bkrepo.scanner.pojo.enums.PlanType
import com.tencent.bkrepo.scanner.pojo.enums.RuleType
import com.tencent.bkrepo.scanner.pojo.enums.ScanStatus
import com.tencent.bkrepo.scanner.pojo.enums.ScanTool
import com.tencent.bkrepo.scanner.pojo.enums.TriggerType
import com.tencent.bkrepo.scanner.pojo.request.ArtifactRule
import com.tencent.bkrepo.scanner.pojo.request.AtomScanRequest
import com.tencent.bkrepo.scanner.pojo.request.BatchScanRequest
import com.tencent.bkrepo.scanner.pojo.request.Rule
import com.tencent.bkrepo.scanner.pojo.request.SingleScanRequest
import com.tencent.bkrepo.scanner.service.ScanService
import com.tencent.bkrepo.scanner.service.container.DockerRunTime
import com.tencent.bkrepo.scanner.service.container.ScanTask
import com.tencent.bkrepo.scanner.util.ScanParamUtil
import org.slf4j.LoggerFactory
import org.springframework.data.domain.Sort
import org.springframework.data.mongodb.core.MongoTemplate
import org.springframework.data.mongodb.core.query.Criteria
import org.springframework.data.mongodb.core.query.Query
import org.springframework.data.mongodb.core.query.Update
import org.springframework.stereotype.Service
import java.time.LocalDateTime
import java.util.UUID

@Service
class ScanServiceImpl(
    private val scanPlanDao: ScanPlanDao,
    private val planRecordDao: PlanRecordDao,
    private val scanResultCveDao: ScanResultCveDao,
    private val mongoTemplate: MongoTemplate,
    private val nodeClient: NodeClient,
    private val repositoryClient: RepositoryClient,
    private val packageClient: PackageClient,
    private val scanTask: ScanTask,
    private val dockerRunTime: DockerRunTime
) : ScanService {

    override fun batchScan(userId: String, request: BatchScanRequest): Boolean {
        logger.info("batchScan userId:$userId, request:$request")
        with(request) {
            //获取方案
            val plan = getScanPlan(projectId, id)
            logger.info("plan:$plan, planTye:${plan.type}")
            val scanRecordList = mutableSetOf<TPlanRecord>()
            //扫描所有仓库
            if (repoNameList.isEmpty()) {
                when (plan.type) {
                    //移动包方案
                    PlanType.MOBILE -> {
                        logger.info("scan all GENERIC repo")
                        //获取所有有权限二进制仓库，
                        val repoList = repositoryClient.listUserRepo(
                            projectId = projectId,
                            userId = userId,
                            type = RepositoryType.GENERIC.toString()
                        ).data ?: return true
                        logger.info("user[$userId] has permission GENERIC repo:$repoList")
                        val repoNameList = repoList.map { it.name }

                        //遍历GENERIC仓库获取符合规则的制品
                        val recordList = getGenericRepoRecord(
                            userId = userId,
                            projectId = projectId,
                            repoNameList = repoNameList,
                            artifactRules = artifactRules,
                            request = request
                        )
                        scanRecordList.addAll(recordList)
                    }

                    //依赖包方案
                    PlanType.DEPENDENT -> {
                        logger.info("scan all MAVEN repo")
                        //获取所有有权限Maven仓库
                        val repoList = repositoryClient.listUserRepo(
                            projectId = projectId,
                            userId = userId,
                            type = RepositoryType.MAVEN.toString()
                        ).data ?: return true
                        logger.info("user has permission MAVEN repo:$repoList")
                        val repoNameList = repoList.map { it.name }

                        //遍历Maven仓库获取符合规则的制品
                        val mavenScanRecord = getMavenRepoRecord(
                            userId = userId,
                            projectId = projectId,
                            repoNameList = repoNameList,
                            artifactRules = artifactRules,
                            request = request
                        )
                        scanRecordList.addAll(mavenScanRecord)
                    }
                }
            } else {
                //扫描指定仓库
                when (plan.type) {
                    //移动包方案
                    PlanType.MOBILE -> {
                        logger.info("scan GENERIC repo:$repoNameList")
                        //遍历仓库获取符合规则的制品
                        val recordList = getGenericRepoRecord(
                            userId = userId,
                            projectId = projectId,
                            repoNameList = repoNameList,
                            artifactRules = artifactRules,
                            request = request
                        )
                        scanRecordList.addAll(recordList)
                    }

                    //依赖包方案
                    PlanType.DEPENDENT -> {
                        logger.info("scan MAVEN repo:$repoNameList")
                        //遍历Maven仓库获取符合规则的制品
                        val mavenScanRecord = getMavenRepoRecord(
                            userId = userId,
                            projectId = projectId,
                            repoNameList = repoNameList,
                            artifactRules = artifactRules,
                            request = request
                        )
                        scanRecordList.addAll(mavenScanRecord)
                    }
                }
            }
            logger.info("projectId:$projectId, planId:$id, scanRecordList:${scanRecordList.toJsonString()}")

            if (scanRecordList.isEmpty())
                throw ArtifactNotMatchException("No match to artifact")

            // 正在扫描/准备扫描 的不重复扫，移除
            removeRepeatScan(scanRecordList)

            //删除历史记录
            delHistoryRecord(scanRecordList)

            //要扫描的制品信息入库，初始化扫描记录数据
            planRecordDao.insert(scanRecordList)

            //调用扫描
            logger.info("start scan")
            //val scanList = scanRecordList.sortedByDescending { it.createdDate }
            //排序处理，保持与列表页面显示顺序一致
            val scanList = scanRecordList.sortedWith(
                compareBy({ it.createdDate }, { it.repoName }, { it.fullPath }, { it.packageKey }, { it.version })
            )
            logger.info("scanList:${scanList.toJsonString()}")
            scanTask.batchScan(scanList)
        }
        return true
    }

    /**
     * 移除重复扫描
     */
    fun removeRepeatScan(scanRecordList: MutableSet<TPlanRecord>) {
        // 正在扫描/准备扫描 的不重复扫
        val runningList = mutableSetOf<TPlanRecord>()
        scanRecordList.forEach { record ->
            val criteria = updateRecordCriteria(record)
            criteria.and(TPlanRecord::scanStatus.name).`in`(listOf(ScanStatus.RUNNING.name, ScanStatus.INIT.name))
            val query = Query(criteria)
            logger.info("getRunningRecord query:$query")
            val runningRecord = mongoTemplate.findOne(query, TPlanRecord::class.java)
            runningRecord?.let {
                runningList.add(record)
            }
        }
        logger.info("runningList:${runningList.toJsonString()}")
        val removeAll = scanRecordList.removeAll(runningList)
        logger.info("scanRecordList:${scanRecordList.toJsonString()}, removeAll:$removeAll")
        if (scanRecordList.isEmpty())
            throw RepeatScanException("No repeat scan for scanning / preparing to scan")
    }

    fun updateRecordCriteria(record: TPlanRecord): Criteria {
        val criteria = Criteria.where(TPlanRecord::projectId.name).`is`(record.projectId)
            .and(TPlanRecord::planId.name).`is`(record.planId)
            .and(TPlanRecord::repoName.name).`is`(record.repoName)
            .and(TPlanRecord::delete.name).`is`(false)
        when (record.repoType) {
            RepositoryType.GENERIC.name -> {
                criteria.and(TPlanRecord::fullPath.name).`is`(record.fullPath)
            }
            RepositoryType.MAVEN.name -> {
                criteria.and(TPlanRecord::packageKey.name).`is`(record.packageKey)
                criteria.and(TPlanRecord::version.name).`is`(record.version)
            }
        }
        return criteria
    }

    /**
     * 删除方案下同一个仓库制品历史扫描记录
     */
    private fun delHistoryRecord(scanRecordList: MutableSet<TPlanRecord>) {
        logger.info("delHistoryRecord")
        scanRecordList.forEach { record ->
            val criteria = updateRecordCriteria(record)
            val query = Query(criteria)
            logger.info("delHistoryRecord query:$query")
            val update = Update().set(TPlanRecord::delete.name, true)
                .set(TPlanRecord::lastModifiedBy.name, record.createdBy)
                .set(TPlanRecord::lastModifiedDate.name, LocalDateTime.now())
            val result = mongoTemplate.updateMulti(query, update, TPlanRecord::class.java)
            logger.info("update result:${result.toJsonString()}")
        }
    }

    override fun atomScan(userId: String, request: AtomScanRequest) {
        logger.info("userId:$userId, request:${request.toJsonString()}")
        request.run {
            //获取当前项目下所有方案(移动端方案/依赖包方案)
            val scanPlanList = getAutoScanPlan(projectId, repoType)
            val planIdList = mutableSetOf<String>()
            //遍历方案，筛选可以扫描该制品的方案
            scanPlanList.forEach { plan ->
                //仓库匹配
                val repoNameList = plan.repoNameList
                logger.info("scan repo:$repoNameList")
                //扫描所有仓库
                if (repoNameList.isEmpty() && artifactIsMatch(plan, request)) {
                    planIdList.add(plan.id!!)
                } else if (repoNameList.contains(repoName) && artifactIsMatch(plan, request)) {
                    //扫描指定仓库，仓库列表包含本次更新仓库
                    planIdList.add(plan.id!!)
                } else {
                    logger.info("The plan cannot match the current update artifact")
                }
            }
            if (planIdList.isEmpty()) {
                logger.info("Scan plan not matched")
                return
            }
            logger.info("planIdList:$planIdList")
            val scanRecordList = mutableSetOf<TPlanRecord>()

            planIdList.forEach { planId ->
                scanRecordList.add(
                    TPlanRecord(
                        id = UUID.randomUUID().toString(),
                        createdBy = userId,
                        createdDate = LocalDateTime.now(),
                        lastModifiedBy = userId,
                        lastModifiedDate = LocalDateTime.now(),
                        planId = planId,
                        triggerMethod = TriggerType.AUTOM.name,
                        projectId = projectId,
                        artifactName = artifactName,
                        packageKey = packageKey,
                        version = version,
                        fullPath = fullPath,
                        repoName = repoName,
                        repoType = repoType.name,
                        scanStatus = ScanStatus.INIT.name,
                        scanTool = ScanTool.TENCENT_SCANNER.toString()
                    )
                )
            }

            // 正在扫描/准备扫描 的不重复扫，移除
            removeRepeatScan(scanRecordList)

            //删除历史记录
            delHistoryRecord(scanRecordList)

            planRecordDao.insert(scanRecordList)

            scanTask.atomScan(scanRecordList.toList())
        }
    }

    override fun singleScan(userId: String, request: SingleScanRequest): Boolean {
        logger.info("userId:$userId, request:${request.toJsonString()}")
        with(request) {
            ScanParamUtil.checkParam(
                repoType = repoType,
                artifactName = name,
                packageKey = packageKey,
                version = version,
                fullPath = fullPath
            )

            //检查仓库是否存在
            repositoryClient.getRepoDetail(projectId, repoName).data ?: throw RepoNotFoundException(repoName)

            //检查节点是否存在
            when (repoType) {
                RepositoryType.MAVEN -> {
                    //maven仓库，查找fullPath
                    val packageVersion = packageClient.findVersionByName(
                        projectId = projectId,
                        repoName = repoName,
                        packageKey = packageKey!!,
                        version = version!!
                    ).data ?: throw NotFoundException(ArtifactMessageCode.VERSION_NOT_FOUND, version)
                    logger.info("packageVersion:$packageVersion")
                    if (!nodeClient.checkExist(projectId, repoName, packageVersion.contentPath!!).data!!) {
                        throw PackageNotFoundException(name)
                    }
                }
                RepositoryType.GENERIC -> {
                    if (!nodeClient.checkExist(projectId, repoName, fullPath!!).data!!) {
                        throw NodeNotFoundException(fullPath!!)
                    }
                }
            }

            val scanRecordList = mutableSetOf(
                TPlanRecord(
                    id = UUID.randomUUID().toString(),
                    createdBy = userId,
                    createdDate = LocalDateTime.now(),
                    lastModifiedBy = userId,
                    lastModifiedDate = LocalDateTime.now(),
                    planId = id,
                    triggerMethod = TriggerType.MANUAL.name,
                    projectId = projectId,
                    artifactName = name,
                    packageKey = packageKey,
                    version = version,
                    fullPath = fullPath,
                    repoName = repoName,
                    repoType = repoType.name,
                    scanStatus = ScanStatus.INIT.name,
                    scanTool = ScanTool.TENCENT_SCANNER.toString()
                )
            )

            // 正在扫描/准备扫描 的不重复扫，移除
            removeRepeatScan(scanRecordList)

            //删除历史记录
            delHistoryRecord(scanRecordList)

            planRecordDao.insert(scanRecordList)

            //调用扫描
            scanTask.batchScan(scanRecordList.toList())
        }

        return true
    }

    /**
     * 更新制品是否匹配方案扫描规则
     */
    private fun artifactIsMatch(plan: TScanPlan, request: AtomScanRequest): Boolean {
        with(request) {
            //制品过滤
            val rules = plan.artifactRules
            logger.info("rules:$rules")
            if (rules.isEmpty()) {
                return true
            } else {
                //多个规则
                rules.forEach { rule ->
                    val nameRule = rule.nameRule
                    val versionRule = rule.versionRule
                    //名称 或 版本规则 为空，匹配其中一个
                    if (nameRule == null || versionRule == null) {
                        //匹配名称规则，只有名称规则，扫描对应的名称的所有版本
                        nameRule?.let {
                            logger.info("name[$artifactName] match")
                            if (matchRule(it, artifactName)) return true
                            /*
                            return when(plan.type) {
                                PlanType.DEPENDENT -> {
                                    matchRule(it, artifactName)
                                }
                                PlanType.MOBILE -> {
                                    matchRule(it, fullPath!!)
                                }
                            }*/
                        }
                        //匹配版本规则
                        versionRule?.let {
                            logger.info("version[$version] match")
                            if (matchRule(versionRule, version!!)) return true
                        }
                    } else {//名称和版本规则都有，“与”关系
                        /*val nameMatch = when(plan.type) {
                            PlanType.DEPENDENT -> {
                                matchRule(nameRule, artifactName)
                            }
                            PlanType.MOBILE -> {
                                matchRule(nameRule, fullPath!!)
                            }
                        }
                        if (nameMatch && matchRule(versionRule, version!!)) {
                            return true
                        }*/
                        logger.info("name[$artifactName] and version[$version] match")
                        if (matchRule(nameRule, artifactName) && matchRule(versionRule, version!!)) {
                            return true
                        }
                    }
                }
            }
        }
        logger.info("plan[${plan.id}] is not supported [${request.artifactName}] atom scan")
        return false
    }

    private fun matchRule(rule: Rule, artifactKey: String): Boolean {
        logger.info("rule:$rule, artifactKey:$artifactKey")
        return when (rule.type) {
            RuleType.EQ -> {
                rule.value == artifactKey
            }
            RuleType.REGEX -> {
                val regex = Regex(rule.value)
                regex.containsMatchIn(artifactKey)
            }
            RuleType.IN -> {
                artifactKey.contains(rule.value)
            }
        }
    }

    /**
     * 获取自动扫描方案
     */
    private fun getAutoScanPlan(projectId: String, repoType: RepositoryType): List<TScanPlan> {
        val type = when (repoType) {
            RepositoryType.GENERIC -> PlanType.MOBILE
            RepositoryType.MAVEN -> PlanType.DEPENDENT
            else -> throw UnsupportedOperationException("repoType:$repoType unsupported scan")
        }
        val query = Query.query(
            Criteria.where(TScanPlan::projectId.name).`is`(projectId)
                .and(TScanPlan::type.name).`is`(type.name)
                .and(TScanPlan::autoScan.name).`is`(true)
                .and(TScanPlan::delete.name).`is`(false)
        )
        val list = mongoTemplate.find(query, TScanPlan::class.java)
        logger.info("AutoScanPlan:${list.toJsonString()}")
        return list
    }

    /**
     * 获取MAVEN仓库扫描记录
     */
    private fun getMavenRepoRecord(
        userId: String,
        projectId: String,
        repoNameList: List<String>,
        artifactRules: List<ArtifactRule>,
        request: BatchScanRequest
    ): Set<TPlanRecord> {
        val scanRecordList = mutableSetOf<TPlanRecord>()
        //遍历所有有权限Maven仓库
        repoNameList.forEach { repoName ->
            //当前仓库所有包
            val packageList = getPackageList(projectId, repoName)
            logger.info("artifactRules:$artifactRules")
            //制品规则为空，扫描所有制品包
            if (artifactRules.isEmpty()) {
                logger.info("get repo[$repoName] latest version package")
                //获取当前仓库制品所有maven包最新版本
                val latestList = packageList.map {
                    convertToPlanRecord(
                        userId = userId,
                        request = request,
                        repoName = repoName,
                        repoType = RepositoryType.MAVEN.toString(),
                        artifactName = it.name,
                        packageKey = it.key,
                        version = it.latest
                    )
                }
                scanRecordList.addAll(latestList)
            } else {//扫描满足规则制品包
                //按规则筛选出maven包
                logger.info("get package by rules")
                artifactRules.forEach { artifactRule ->
                    val recordList = getPackageByRegex(
                        userId = userId,
                        request = request,
                        artifactRule = artifactRule,
                        packageList = packageList
                    )
                    scanRecordList.addAll(recordList)
                }
            }
        }

        return scanRecordList
    }

    /**
     * 获取二进制仓库扫描记录
     */
    private fun getGenericRepoRecord(
        userId: String,
        projectId: String,
        repoNameList: List<String>,
        artifactRules: List<ArtifactRule>,
        request: BatchScanRequest
    ): List<TPlanRecord> {
        val scanNodeList = mutableSetOf<NodeInfo>()
        //遍历所有有权限二进制仓库
        repoNameList.forEach { repoName ->
            //获取当前仓库制品所有ipa/apk制品
            val nodeList = getRepoNode(projectId, repoName).filter { node ->
                node.name.endsWith(".apk") || node.name.endsWith(".ipa")
            }

            logger.info("ipa/apk nodeList:${nodeList.toJsonString()}, node size:${nodeList.size}")
            if (artifactRules.isEmpty()) {
                //扫描所有制品包(移动端安装包质量扫描方案仅支持扫描 Generic 类型制品仓库下的 ipa，apk 类型制品)
                scanNodeList.addAll(nodeList)
            } else {
                //扫描满足规则制品包
                //按规则筛选出ipa/apk包，二进制仓库只有名称规则，无版本规则
                artifactRules.forEach { rule ->
                    scanNodeList.addAll(getNodeByRegex(rule, nodeList))
                }
            }
            logger.info("repoName:$repoName, artifactRules:$artifactRules, scanNodeList:$scanNodeList")
        }
        return scanNodeList.map { node ->
            convertToPlanRecord(
                userId = userId,
                request = request,
                repoName = node.repoName,
                repoType = RepositoryType.GENERIC.toString(),
                fullPath = node.fullPath,
                artifactName = node.name
            )
        }
    }

    private fun getScanPlan(projectId: String, id: String?): TScanPlan {
        return scanPlanDao.getPlan(projectId, null, id) ?: run {
            logger.error("ScanPlan not exist:$projectId, $id")
            throw PlanNotFoundException(id!!)
        }
    }

    private fun convertToPlanRecord(
        userId: String,
        request: BatchScanRequest,
        repoName: String,
        repoType: String,
        artifactName: String,
        fullPath: String? = null,
        packageKey: String? = null,
        version: String? = null
    ): TPlanRecord {
        return TPlanRecord(
            id = UUID.randomUUID().toString(),
            createdBy = userId,
            createdDate = LocalDateTime.now(),
            lastModifiedBy = userId,
            lastModifiedDate = LocalDateTime.now(),
            planId = request.id,
            triggerMethod = request.triggerMethod.toString(),
            projectId = request.projectId,
            artifactName = artifactName,
            packageKey = packageKey,
            version = version,
            fullPath = fullPath,
            repoName = repoName,
            repoType = repoType,
            scanStatus = ScanStatus.INIT.toString(),
            scanTool = ScanTool.TENCENT_SCANNER.toString()
        )
    }

    /**
     * 正则匹配当前仓库所有制品
     */
    private fun getPackageByRegex(
        userId: String,
        request: BatchScanRequest,
        artifactRule: ArtifactRule,
        packageList: List<PackageSummary>
    ): Set<TPlanRecord> {
        val resultList = mutableSetOf<TPlanRecord>()

        logger.info("packageList:$packageList, artifactRule:$artifactRule")
        val nameRule = artifactRule.nameRule
        val versionRule = artifactRule.versionRule
        packageList.forEach { packageSummary ->
            //名称 或 版本规则 为空，匹配其中一个
            if (nameRule == null || versionRule == null) {
                //匹配名称规则，只有名称规则，扫描对应的名称的所有版本
                nameRule?.let {
                    logger.info("match by name:$it")
                    if (matchRule(it, packageSummary.name)) {
                        val versionRecord = packageSummary.historyVersion.map { version ->
                            convertToPlanRecord(
                                userId = userId,
                                request = request,
                                repoName = packageSummary.repoName,
                                repoType = RepositoryType.MAVEN.toString(),
                                artifactName = packageSummary.name,
                                packageKey = packageSummary.key,
                                version = version
                            )
                        }
                        resultList.addAll(versionRecord)
                    }
                }
                //匹配版本规则
                versionRule?.let { versionRegex ->
                    val versionRecord = getVersionRecord(
                        versionRegex = versionRegex,
                        userId = userId,
                        request = request,
                        packageSummary = packageSummary
                    )
                    resultList.addAll(versionRecord)
                }
            } else {//名称和版本规则都有，“与”关系
                //匹配名称
                if (matchRule(nameRule, packageSummary.name)) {
                    //匹配版本
                    val versionRecord = getVersionRecord(
                        versionRegex = versionRule,
                        userId = userId,
                        request = request,
                        packageSummary = packageSummary
                    )
                    resultList.addAll(versionRecord)
                }
            }
            logger.info("packageSummary:$packageSummary, resultList:$resultList")
        }
        logger.info("resultList:$resultList")
        return resultList
    }

    private fun getVersionRecord(
        versionRegex: Rule,
        userId: String,
        request: BatchScanRequest,
        packageSummary: PackageSummary
    ): List<TPlanRecord> {
        //符合条件的历史版本
        val versions = packageSummary.historyVersion.filter { version ->
            matchRule(versionRegex, version)
        }
        logger.info("Eligible historical versions:$versions")
        return versions.map { version ->
            convertToPlanRecord(
                userId = userId,
                request = request,
                repoName = packageSummary.repoName,
                repoType = RepositoryType.MAVEN.toString(),
                artifactName = packageSummary.name,
                packageKey = packageSummary.key,
                version = version
            )
        }
    }

    /**
     * 仓库下所有包
     */
    private fun getPackageList(projectId: String, repoName: String): List<PackageSummary> {
        logger.info("getPackageList projectId[${projectId}/${repoName}]")
        val resultList = mutableListOf<PackageSummary>()
        // 查询包
        var pageNumber = 1
        var packageOption = PackageListOption(pageNumber = pageNumber, pageSize = DEFAULT_PAGE_SIZE)
        var packagePage = packageClient.listPackagePage(
            projectId = projectId,
            repoName = repoName,
            option = packageOption
        ).data ?: return resultList
        while (packagePage.records.isNotEmpty()) {
            resultList.addAll(packagePage.records)
            pageNumber += 1
            packageOption = PackageListOption(pageNumber = pageNumber, pageSize = DEFAULT_PAGE_SIZE)
            packagePage = packageClient.listPackagePage(
                projectId = projectId,
                repoName = repoName,
                option = packageOption
            ).data ?: return resultList
        }
        logger.info("resultList:$resultList")
        return resultList
    }

    private fun getNodeByRegex(rule: ArtifactRule, nodeList: List<NodeInfo>): Set<NodeInfo> {
        val scanNodeList = mutableSetOf<NodeInfo>()
        nodeList.forEach { node ->
            rule.nameRule?.let {
                //制品全路径按规则筛选
                /*if (matchRule(it, node.fullPath)) {
                    scanNodeList.add(node)
                }*/
                if (matchRule(it, node.name)) {
                    scanNodeList.add(node)
                }
            }
        }
        logger.info("scanNodeList:$scanNodeList")
        return scanNodeList
    }

    /**
     * 查询GENERIC仓库下所有制品
     */
    private fun getRepoNode(projectId: String, repoName: String): Set<NodeInfo> {
        var pageNumber = 1
        val nodeList = mutableSetOf<NodeInfo>()
        do {
            val option = NodeListOption(
                pageNumber = pageNumber,
                pageSize = DEFAULT_PAGE_SIZE,
                includeFolder = false,
                deep = true,
                includeMetadata = true,
                sort = true
            )
            val nodePage = nodeClient.listNodePage(
                projectId = projectId,
                repoName = repoName,
                path = "/",
                option = option
            ).data ?: return nodeList
            nodeList.addAll(nodePage.records)
            logger.info("nodePage:$nodePage, nodeList:${nodeList.toJsonString()}, nodeList.size:${nodeList.size}")
            pageNumber++
        } while (nodeList.size < nodePage.totalRecords)
        logger.info("repo[$repoName], nodeList:${nodeList.toJsonString()}")

        return nodeList
    }

    override fun stopScan(userId: String, projectId: String, recordId: String): Boolean {
        val criteria = Criteria.where(TPlanRecord::projectId.name).`is`(projectId)
            .and(TPlanRecord::id.name).`is`(recordId)
            .and(TPlanRecord::delete.name).`is`(false)
            .and(TPlanRecord::scanStatus.name).`in`(listOf(ScanStatus.INIT.name, ScanStatus.RUNNING.name))

        val query = Query(criteria)
        logger.info("get containerId to stop container, query:$query")
        val record = mongoTemplate.findOne(query, TPlanRecord::class.java)
            ?: throw MethodNotAllowedException("End of scan, not allowed stop")
        logger.info("stop scan record:${record.toJsonString()}")

        val update = Update().set(TPlanRecord::scanStatus.name, ScanStatus.STOP.toString())
            .set(TPlanRecord::lastModifiedBy.name, userId)
            .set(TPlanRecord::lastModifiedDate.name, LocalDateTime.now())
        val result = mongoTemplate.updateFirst(query, update, TPlanRecord::class.java)
        logger.info("update record:${result.toJsonString()}")

        stopContainer(record)

        return result.modifiedCount == 1L
    }

    fun stopContainer(record: TPlanRecord) {
        with(record) {
            //自动触发
            if (triggerMethod == TriggerType.AUTOM.name) {
                //是否还有别的方案在扫描
                val criteria = Criteria.where(TPlanRecord::projectId.name).`is`(projectId)
                    .and(TPlanRecord::delete.name).`is`(false)
                    .and(TPlanRecord::repoName.name).`is`(repoName)
                    .and(TPlanRecord::fullPath.name).`is`(fullPath)
                    .and(TPlanRecord::packageKey.name).`is`(packageKey)
                    .and(TPlanRecord::version.name).`is`(version)
                    .and(TPlanRecord::triggerMethod.name).`is`(TriggerType.AUTOM.name)
                    .and(TPlanRecord::scanStatus.name).`in`(listOf(ScanStatus.INIT.name, ScanStatus.RUNNING.name))
                record.containerId?.let {
                    criteria.and(TPlanRecord::containerId.name).`is`(containerId)
                }
                val query = Query(criteria)
                logger.info("get containerId to stop container, query:$query")
                val recordList = mongoTemplate.find(query, TPlanRecord::class.java)
                logger.info("recordList:${recordList.toJsonString()}")
                if (recordList.isEmpty()
                    && scanStatus == ScanStatus.RUNNING.name
                    && !containerId.isNullOrEmpty()
                ) {
                    logger.info("scanStatus:$scanStatus, stop containerId:$containerId")
                    dockerRunTime.stopContainer(containerId!!)
                }
            } else {//手动触发
                //正在扫描
                if (scanStatus == ScanStatus.RUNNING.name && !containerId.isNullOrEmpty()) {
                    logger.info("scanStatus:$scanStatus, stop containerId:$containerId")
                    dockerRunTime.stopContainer(containerId!!)
                }
            }
        }
    }

    override fun artifactLeak(
        projectId: String,
        recordId: String,
        cveId: String?,
        leakType: LeakType?,
        pageNumber: Int,
        pageSize: Int
    ): Page<ArtifactLeakInfo?> {
        val pageRequest = Pages.ofRequest(pageNumber, pageSize)
        val query = buildScanCvePageQuery(
            projectId = projectId,
            recordId = recordId,
            cveId = cveId,
            leakType = leakType
        )
        logger.info("query:$query")

        val totalRecords = scanResultCveDao.count(query)
        val records = scanResultCveDao.find(query.with(pageRequest)).map { convert(it) }
        return Pages.ofResponse(pageRequest, totalRecords, records)
    }

    override fun artifactCount(projectId: String, recordId: String): ArtifactCountInfo {
        val query = Query.query(
            Criteria.where(TPlanRecord::projectId.name).`is`(projectId)
                .and(TPlanRecord::id.name).`is`(recordId)
                .and(TPlanRecord::delete.name).`is`(false)
        )
        val record = mongoTemplate.findOne(query, TPlanRecord::class.java)
            ?: throw NotFoundException(CommonMessageCode.RESOURCE_NOT_FOUND, recordId)
        return with(record) {
            ArtifactCountInfo(
                recordId = recordId,
                name = artifactName,
                packageKey = packageKey,
                version = version,
                fullPath = fullPath,
                repoType = repoType,
                repoName = repoName,
                highestLeakLevel = highestLeakLevel,
                duration = duration,
                finishTime = lastModifiedDate,
                status = scanStatus,
                critical = critical,
                high = high,
                medium = medium,
                low = low,
                total = critical + high + medium + low
            )
        }
    }

    private fun convert(cve: TScanResultCve?): ArtifactLeakInfo? {
        return cve?.let {
            ArtifactLeakInfo(
                cveId = it.cveId,
                severity = it.severity,
                pkgName = it.pkgName,
                installedVersion = it.installedVersion,
                title = it.title,
                description = it.description,
                officialSolution = it.officialSolution,
                reference = it.reference
            )
        }
    }

    private fun buildScanCvePageQuery(
        projectId: String,
        recordId: String,
        cveId: String?,
        leakType: LeakType?
    ): Query {
        val criteria = Criteria.where(TScanResultCve::recordId.name).`is`(recordId)
        cveId?.let { criteria.and(TScanResultCve::cveId.name).`is`(cveId) }
        leakType?.let { criteria.and(TScanResultCve::severity.name).`is`(leakType.name) }
        return Query(criteria).with(Sort.by(TScanResultCve::createdDate.name).descending())
    }


    companion object {
        private const val DEFAULT_PAGE_SIZE = 10000
        private val logger = LoggerFactory.getLogger(ScanServiceImpl::class.java)

    }
}
