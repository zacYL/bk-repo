package com.tencent.bkrepo.opdata.repository

import com.tencent.bkrepo.opdata.model.TVersionOp
import org.springframework.data.mongodb.repository.MongoRepository
import org.springframework.stereotype.Repository

@Repository
interface VersionOpRepository : MongoRepository<TVersionOp, String>
