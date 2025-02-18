package com.tencent.bkrepo.common.metadata.dao.whitelist

import com.tencent.bkrepo.common.metadata.condition.SyncCondition
import com.tencent.bkrepo.common.mongo.dao.simple.SimpleMongoDao
import com.tencent.bkrepo.common.metadata.model.TWhitelistSwitch
import org.springframework.context.annotation.Conditional
import org.springframework.stereotype.Repository

@Repository
@Conditional(SyncCondition::class)
class WhitelistSwitchDao: SimpleMongoDao<TWhitelistSwitch>()
