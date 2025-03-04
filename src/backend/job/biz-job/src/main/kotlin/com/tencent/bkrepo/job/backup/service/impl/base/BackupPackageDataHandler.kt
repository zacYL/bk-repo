package com.tencent.bkrepo.job.backup.service.impl.base

import com.tencent.bkrepo.common.artifact.constant.REPO_NAME
import com.tencent.bkrepo.job.PROJECT
import com.tencent.bkrepo.job.backup.pojo.query.BackupPackageInfo
import com.tencent.bkrepo.job.backup.pojo.query.enums.BackupDataEnum
import com.tencent.bkrepo.job.backup.pojo.record.BackupContext
import com.tencent.bkrepo.job.backup.pojo.setting.BackupConflictStrategy
import com.tencent.bkrepo.job.backup.service.BackupDataHandler
import com.tencent.bkrepo.repository.api.PackageClient
import org.slf4j.LoggerFactory
import org.springframework.data.mongodb.core.MongoTemplate
import org.springframework.data.mongodb.core.query.Criteria
import org.springframework.data.mongodb.core.query.Query
import org.springframework.data.mongodb.core.query.Update
import org.springframework.data.mongodb.core.query.isEqualTo
import org.springframework.stereotype.Component

@Component
class BackupPackageDataHandler(
    private val mongoTemplate: MongoTemplate,
    private val packageClient: PackageClient,
) : BackupDataHandler {
    override fun dataType(): BackupDataEnum {
        return BackupDataEnum.PACKAGE_DATA
    }

    override fun buildQueryCriteria(context: BackupContext): Criteria {
        val criteria = Criteria.where(PROJECT).isEqualTo(context.currentProjectId)
            .and(REPO_NAME).isEqualTo(context.currentRepoName)
        if (context.incrementDate != null) {
            criteria.and(BackupPackageInfo::lastModifiedDate.name).gte(context.incrementDate!!)
        }
        return criteria
    }

    override fun <T> preBackupDataHandler(record: T, backupDataEnum: BackupDataEnum, context: BackupContext) {
        val packageInfo = record as BackupPackageInfo
        context.currentPackageId = record.id
        context.currentPackageKey = record.key
    }

    override fun <T> returnLastId(data: T): String {
        return (data as BackupPackageInfo).id!!
    }

    override fun <T> storeRestoreDataHandler(record: T, backupDataEnum: BackupDataEnum, context: BackupContext) {
        val record = record as BackupPackageInfo
        val existRecord = findExistPackage(record)
        if (existRecord != null) {
            if (context.task.backupSetting.conflictStrategy == BackupConflictStrategy.SKIP) {
                updatePackageInfo(record)
                return
            }
            updateExistPackage(record)
        } else {
            mongoTemplate.save(record, BackupDataEnum.PACKAGE_DATA.collectionName)
            logger.info("Create package ${record.key} in ${record.projectId}|${record.name} success!")
        }
    }

    //TODO  依赖源节点恢复时需要考虑索引文件如何更新.(仓库索引/包索引等 )
    private fun findExistPackage(record: BackupPackageInfo): BackupPackageInfo? {
        val existPackageQuery = buildQuery(record)
        return mongoTemplate.findOne(
            existPackageQuery,
            BackupPackageInfo::class.java,
            BackupDataEnum.PACKAGE_DATA.collectionName
        )
    }

    private fun updateExistPackage(packageInfo: BackupPackageInfo) {
        val packageQuery = buildQuery(packageInfo)
        // 逻辑删除， 同时删除索引
        val update = Update()
            .set(BackupPackageInfo::latest.name, packageInfo.latest)
            .set(BackupPackageInfo::description.name, packageInfo.description)
            .set(BackupPackageInfo::extension.name, packageInfo.extension)
            .set(BackupPackageInfo::clusterNames.name, packageInfo.clusterNames)

        mongoTemplate.updateFirst(packageQuery, update, BackupDataEnum.PACKAGE_DATA.collectionName)
        logger.info(
            "update exist package ${packageInfo.key} " +
                    "success with name ${packageInfo.projectId}|${packageInfo.repoName}"
        )
    }

    private fun updatePackageInfo(packageInfo: BackupPackageInfo) {
        // 跳过 包的信息需要重新计算，更新最新版本和版本数
        val packageVersions =
            packageClient.listAllVersion(packageInfo.projectId, packageInfo.repoName, packageInfo.key).data ?: return
        // 返回列表已默认根据版本排序，取第一个为最新
        val lastVersion = if (packageVersions.isEmpty()) null else packageVersions[0]
        val packageQuery = buildQuery(packageInfo)
        val update = Update()
            .set(BackupPackageInfo::latest.name, lastVersion?.name)
            .set(BackupPackageInfo::versions.name, packageVersions.size)

        mongoTemplate.updateFirst(packageQuery, update, BackupDataEnum.PACKAGE_DATA.collectionName)
        logger.info(
            "recount package info ${packageInfo.key} " +
                    "success with name ${packageInfo.projectId}|${packageInfo.repoName}"
        )
    }

    private fun buildQuery(packageInfo: BackupPackageInfo): Query {
        return Query(
            Criteria.where(BackupPackageInfo::projectId.name).isEqualTo(packageInfo.projectId)
                .and(BackupPackageInfo::repoName.name).isEqualTo(packageInfo.repoName)
                .and(BackupPackageInfo::key.name).isEqualTo(packageInfo.key)
        )
    }

    companion object {
        private val logger = LoggerFactory.getLogger(BackupPackageDataHandler::class.java)
    }
}