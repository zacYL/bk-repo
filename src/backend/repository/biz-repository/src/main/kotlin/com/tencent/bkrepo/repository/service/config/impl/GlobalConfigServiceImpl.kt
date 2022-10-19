package com.tencent.bkrepo.repository.service.config.impl

import com.tencent.bkrepo.repository.dao.repository.GlobalConfigRepository
import com.tencent.bkrepo.repository.model.TGlobalConfig
import com.tencent.bkrepo.repository.pojo.config.GlobalConfigInfo
import com.tencent.bkrepo.repository.service.config.GlobalConfigService
import org.slf4j.LoggerFactory
import org.springframework.data.domain.Sort
import org.springframework.stereotype.Service
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/**
 * 全局配置服务
 */

@Service
class GlobalConfigServiceImpl(
    private val globalConfigRepository: GlobalConfigRepository
) : GlobalConfigService {
    override fun updateConfig(userId: String, map: Map<String, Any>): GlobalConfigInfo? {
        val globalConfig = globalConfigRepository.findAll(
            Sort.by(Sort.Direction.ASC, TGlobalConfig::createdDate.name)
        ).firstOrNull()
        val nowDate = LocalDateTime.now()
        val tGlobalConfig = TGlobalConfig(
            id = globalConfig?.id,
            createdBy = globalConfig?.createdBy ?: userId,
            createdDate = globalConfig?.createdDate ?: nowDate,
            lastModifiedBy = userId,
            lastModifiedDate = nowDate,
            replicationNetworkRate = if (map[TGlobalConfig::replicationNetworkRate.name] != null) {
                map[TGlobalConfig::replicationNetworkRate.name].toString().toLong()
            } else if (globalConfig?.replicationNetworkRate != null) {
                globalConfig.replicationNetworkRate
            } else {
                null
            }
        )
        logger.info("update config success by $userId:[$tGlobalConfig]")
        return convert(globalConfigRepository.save(tGlobalConfig))
    }

    override fun getConfig(): GlobalConfigInfo? {
        val globalConfig = globalConfigRepository.findAll(
            Sort.by(Sort.Direction.ASC, TGlobalConfig::createdDate.name)
        ).firstOrNull()
        return convert(globalConfig)
    }

    companion object {
        private val logger = LoggerFactory.getLogger(GlobalConfigServiceImpl::class.java)

        fun convert(tGlobalConfig: TGlobalConfig?): GlobalConfigInfo? {
            return tGlobalConfig?.let {
                GlobalConfigInfo(
                    createdBy = it.createdBy,
                    createdDate = it.createdDate.format(DateTimeFormatter.ISO_DATE_TIME),
                    lastModifiedBy = it.lastModifiedBy,
                    lastModifiedDate = it.lastModifiedDate.format(DateTimeFormatter.ISO_DATE_TIME),
                    replicationNetworkRate = it.replicationNetworkRate
                )
            }
        }
    }
}
