package com.tencent.bkrepo.repository.dao

import com.tencent.bkrepo.common.mongo.dao.simple.SimpleMongoDao
import com.tencent.bkrepo.repository.model.TGlobalConfig
import com.tencent.bkrepo.repository.pojo.config.ConfigType
import org.springframework.data.mongodb.core.query.Query
import org.springframework.data.mongodb.core.query.isEqualTo
import org.springframework.stereotype.Repository

/**
 * 全局配置数据访问层
 */
@Repository
class GlobalConfigDao : SimpleMongoDao<TGlobalConfig>() {
    fun findByType(type: ConfigType): TGlobalConfig? {
        return this.findOne(Query(TGlobalConfig::type.isEqualTo(type)))
    }
}
