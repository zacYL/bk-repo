package com.tencent.bkrepo.repository.service.config

import com.tencent.bkrepo.repository.pojo.config.ConfigType
import com.tencent.bkrepo.repository.pojo.config.GlobalConfigInfo
import com.tencent.bkrepo.repository.pojo.config.UserCreateConfigurationRequest

/**
 * 全局配置服务接口
 */
interface GlobalConfigService {
    fun updateConfig(userId: String, request: UserCreateConfigurationRequest): GlobalConfigInfo?

    fun getConfig(type: ConfigType): GlobalConfigInfo?
}
