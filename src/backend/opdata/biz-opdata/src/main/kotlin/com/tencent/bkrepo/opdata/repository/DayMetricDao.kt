package com.tencent.bkrepo.opdata.repository

import com.tencent.bkrepo.common.mongo.dao.simple.SimpleMongoDao
import com.tencent.bkrepo.opdata.model.TDayMetric
import org.springframework.stereotype.Repository

@Repository
class DayMetricDao : SimpleMongoDao<TDayMetric>()
