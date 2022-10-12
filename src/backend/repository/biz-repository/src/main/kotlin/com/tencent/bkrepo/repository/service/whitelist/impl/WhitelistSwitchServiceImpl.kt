package com.tencent.bkrepo.repository.service.whitelist.impl

import com.tencent.bkrepo.common.artifact.pojo.RepositoryType
import com.tencent.bkrepo.common.security.util.SecurityUtils
import com.tencent.bkrepo.repository.dao.WhitelistSwitchDao
import com.tencent.bkrepo.repository.model.TWhitelistSwitch
import com.tencent.bkrepo.repository.service.whitelist.WhitelistSwitchService
import com.tencent.bkrepo.repository.util.WhitelistUtils
import org.springframework.data.mongodb.core.query.Criteria
import org.springframework.data.mongodb.core.query.Query
import org.springframework.stereotype.Service
import java.time.LocalDateTime

@Service
class WhitelistSwitchServiceImpl(
    private val whitelistSwitchDao: WhitelistSwitchDao
) : WhitelistSwitchService {

    override fun list(): Map<RepositoryType, Boolean> {
        val enabledTypes = whitelistSwitchDao.findAll().map { it.type }
        return WhitelistUtils.optionalType().associateWith { enabledTypes.contains(it) }
    }


    override fun switch(type: RepositoryType, status: Boolean?): Boolean {
        val query = getTypeQuery(type)
        val exist = whitelistSwitchDao.findOne(query)
        return if (exist == null) {
            whitelistSwitchDao.insert(TWhitelistSwitch(
                    type = type,
                    createdDate = LocalDateTime.now(),
                    createdBy = SecurityUtils.getUserId()
            ))
            true
        } else {
            whitelistSwitchDao.remove(query)
            false
        }
    }

    override fun get(type: RepositoryType): Boolean {
        return whitelistSwitchDao.findOne(getTypeQuery(type)) != null
    }

    private fun getTypeQuery(type: RepositoryType): Query {
        return Query.query(Criteria.where(TWhitelistSwitch::type.name).`is`(type))
    }
}