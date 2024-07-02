package com.tencent.bkrepo.job.batch.base

import com.tencent.bkrepo.job.config.properties.BatchJobProperties

abstract class CenterNodeJob<C : JobContext>(batchJobProperties: BatchJobProperties) : BatchJob<C>(batchJobProperties)
