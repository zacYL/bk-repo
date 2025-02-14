package com.tencent.bkrepo.repository.cpack.service.impl

import com.tencent.bkrepo.common.api.util.JsonUtils
import com.tencent.bkrepo.common.api.util.toJsonString
import com.tencent.bkrepo.common.artifact.pojo.RepositoryCategory
import com.tencent.bkrepo.common.metadata.dao.repo.RepositoryDao
import com.tencent.bkrepo.common.metadata.model.TRepository
import com.tencent.bkrepo.repository.cpack.pojo.repo.OutdatedVirtualConfiguration
import com.tencent.bkrepo.repository.cpack.service.CpackRepairService
import org.slf4j.LoggerFactory
import org.springframework.data.mongodb.core.query.Query
import org.springframework.data.mongodb.core.query.isEqualTo
import org.springframework.data.mongodb.core.query.where
import org.springframework.stereotype.Service

@Service
class CpackRepairServiceImpl(
    private val repositoryDao: RepositoryDao
) : CpackRepairService {
    override fun repairVirtualConfiguration(): List<String> {
        val virtualRepos = repositoryDao.find(Query(where(TRepository::category).isEqualTo(RepositoryCategory.VIRTUAL)))
        logger.info("Retrieve ${virtualRepos.size} virtual repos")
        val repairedRepos = mutableListOf<String>()
        virtualRepos.forEach { repo ->
            val repoId = "${repo.projectId}/${repo.name}"
            var repaired = false
            val config = JsonUtils.objectMapper.readValue(repo.configuration, OutdatedVirtualConfiguration::class.java)
            config.repositoryList.forEach {
                if (it.projectId == null) {
                    logger.info("---- missing projectId: [${it.name}] in $repoId")
                    it.projectId = repo.projectId
                    repaired = true
                }
            }
            repo.configuration = config.toJsonString()
            if (repaired) {
                repositoryDao.save(repo)
                logger.info("Virtual repo [${repo.projectId}/${repo.name}] repaired")
                repairedRepos.add(repoId)
            }
        }
        return repairedRepos
    }

    companion object {
        private val logger = LoggerFactory.getLogger(CpackRepairServiceImpl::class.java)
    }
}
