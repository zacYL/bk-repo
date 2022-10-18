package com.tencent.bkrepo.repository.service.config

import com.tencent.bkrepo.repository.pojo.config.GlobalConfigInfo

/**
 * 全局配置服务接口
 */
interface GlobalConfigService {
    fun updateConfig(userId: String, map: Map<String, Any>): GlobalConfigInfo?

    fun getConfig(): GlobalConfigInfo?
}
