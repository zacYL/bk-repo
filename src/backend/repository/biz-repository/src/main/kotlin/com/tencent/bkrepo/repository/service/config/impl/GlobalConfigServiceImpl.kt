package com.tencent.bkrepo.repository.service.config.impl

import com.tencent.bkrepo.repository.dao.GlobalConfigDao
import com.tencent.bkrepo.repository.model.TGlobalConfig
import com.tencent.bkrepo.repository.pojo.config.ConfigType
import com.tencent.bkrepo.repository.pojo.config.GlobalConfigInfo
import com.tencent.bkrepo.repository.pojo.config.UserCreateConfigurationRequest
import com.tencent.bkrepo.repository.service.config.GlobalConfigService
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/**
 * 全局配置服务
 */

@Service
class GlobalConfigServiceImpl(
    private val globalConfigDao: GlobalConfigDao
) : GlobalConfigService {
    override fun updateConfig(userId: String, request: UserCreateConfigurationRequest): GlobalConfigInfo? {
        val config = globalConfigDao.findByType(request.type)
        val nowDate = LocalDateTime.now()
        val tGlobalConfig = TGlobalConfig(
            id = config?.id,
            createdBy = config?.createdBy ?: userId,
            createdDate = config?.createdDate ?: nowDate,
            lastModifiedBy = userId,
            lastModifiedDate = nowDate,
            type = config?.type ?: request.type,
            configuration = request.configuration,
        )
        logger.info("update config success by $userId:[$tGlobalConfig]")
        return convert(globalConfigDao.save(tGlobalConfig))
    }

    override fun getConfig(type: ConfigType): GlobalConfigInfo? {
        return convert(globalConfigDao.findByType(type))
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
                    type = it.type,
                    configuration = it.configuration
                )
            }
        }
    }
}
