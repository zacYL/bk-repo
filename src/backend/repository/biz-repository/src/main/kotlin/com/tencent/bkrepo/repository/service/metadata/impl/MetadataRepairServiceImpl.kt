package com.tencent.bkrepo.repository.service.metadata.impl

import com.tencent.bkrepo.common.artifact.constant.RESERVED_KEY
import com.tencent.bkrepo.common.metadata.dao.node.NodeDao
import com.tencent.bkrepo.common.metadata.dao.packages.PackageVersionDao
import com.tencent.bkrepo.common.metadata.dao.project.ProjectDao
import com.tencent.bkrepo.common.metadata.model.TNode
import com.tencent.bkrepo.common.metadata.model.TPackageVersion
import com.tencent.bkrepo.repository.service.metadata.MetadataRepairService
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class MetadataRepairServiceImpl(
    private val projectDao: ProjectDao,
    private val nodeDao: NodeDao,
    private val packageVersionDao: PackageVersionDao
) : MetadataRepairService {
    override fun metadataUpdate() {
        var nodeMetadataCount = 0L
        var packageMetadataCount = 0L
        // 进行二进制文件元数据调整
        logger.info("Start adjusting node metadata")
        val projectList = projectDao.findAll()
        projectList.forEach { project ->
            val nodeList = nodeDao.findFileNode(project.name)
            if (nodeList.isNotEmpty()) {
                nodeList.forEach { node ->
                    if (nodeMetedateUpdate(node)) {
                        nodeMetadataCount += 1
                    }
                }
            } else {
                logger.info("[${project.name}] project no node found")
            }
        }
        // 进行包元数据调整
        logger.info("Start adjusting packageVersion metadata")
        val packageVersionList = packageVersionDao.findAll()
        if (packageVersionList.isNotEmpty()) {
            packageVersionList.forEach { packVersion ->
                if (nodeMetedateUpdate(packVersion)) {
                    packageMetadataCount += 1
                }
            }
        } else {
            logger.info("no package found")
        }
        logger.info("Completed [$nodeMetadataCount]nodes and [$packageMetadataCount]packages metadata adjustment")
    }

    fun nodeMetedateUpdate(node: TNode): Boolean {
        with(node) {
            var isUpdate = false
            this.metadata?.forEach {
                if (it.key in RESERVED_KEY) {
                    it.system = true
                    it.display = false
                    isUpdate = true
                }
            }
            if (isUpdate) {
                nodeDao.save(this)
            }
            return isUpdate
        }
    }

    fun nodeMetedateUpdate(packVersion: TPackageVersion): Boolean {
        with(packVersion) {
            this.metadata.forEach {
                it.system = true
                it.display = if (it.key in RESERVED_KEY) false else true
            }
            packageVersionDao.save(this)
            return true
        }
    }

    companion object {
        private val logger: Logger = LoggerFactory.getLogger(MetadataRepairServiceImpl::class.java)
    }
}
