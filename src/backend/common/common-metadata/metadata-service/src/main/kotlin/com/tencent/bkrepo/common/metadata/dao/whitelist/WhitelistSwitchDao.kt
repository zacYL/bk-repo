package com.tencent.bkrepo.common.metadata.dao.whitelist

import com.tencent.bkrepo.common.mongo.dao.simple.SimpleMongoDao
import com.tencent.bkrepo.common.metadata.model.TWhitelistSwitch
import org.springframework.stereotype.Repository

@Repository
class WhitelistSwitchDao: SimpleMongoDao<TWhitelistSwitch>()
