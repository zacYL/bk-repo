package com.tencent.bkrepo.repository.dao.repository

import com.tencent.bkrepo.repository.model.TGlobalConfig
import org.springframework.data.mongodb.repository.MongoRepository
import org.springframework.stereotype.Repository

@Repository
interface GlobalConfigRepository : MongoRepository<TGlobalConfig, String>
