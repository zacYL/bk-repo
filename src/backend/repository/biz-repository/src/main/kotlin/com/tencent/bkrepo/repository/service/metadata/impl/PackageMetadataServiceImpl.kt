/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
 *
 * Copyright (C) 2020 THL A29 Limited, a Tencent company.  All rights reserved.
 *
 * BK-CI 蓝鲸持续集成平台 is licensed under the MIT license.
 *
 * A copy of the MIT License is included in this file.
 *
 *
 * Terms of the MIT License:
 * ---------------------------------------------------
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package com.tencent.bkrepo.repository.service.metadata.impl

import com.tencent.bkrepo.common.api.exception.ErrorCodeException
import com.tencent.bkrepo.common.api.message.CommonMessageCode
import com.tencent.bkrepo.repository.dao.PackageDao
import com.tencent.bkrepo.repository.dao.PackageVersionDao
import com.tencent.bkrepo.repository.model.TMetadata
import com.tencent.bkrepo.repository.model.TPackage
import com.tencent.bkrepo.repository.model.TPackageVersion
import com.tencent.bkrepo.repository.pojo.metadata.packages.PackageMetadataDeleteRequest
import com.tencent.bkrepo.repository.pojo.metadata.packages.PackageMetadataSaveRequest
import com.tencent.bkrepo.repository.service.metadata.PackageMetadataService
import com.tencent.bkrepo.repository.util.MetadataUtils
import com.tencent.bkrepo.repository.util.PackageQueryHelper
import org.slf4j.LoggerFactory
import org.springframework.data.mongodb.core.query.Criteria
import org.springframework.data.mongodb.core.query.Query
import org.springframework.data.mongodb.core.query.Update
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * 元数据服务实现类
 */
@Service
class PackageMetadataServiceImpl(
    private val packageDao: PackageDao,
    private val packageVersionDao: PackageVersionDao
) : PackageMetadataService {

    override fun listMetadata(
        projectId: String,
        repoName: String,
        packageKey: String,
        version: String
    ): Map<String, Any> {
        val tPackage = checkPackage(projectId, repoName, packageKey)
        return MetadataUtils.toMap(packageVersionDao.findByName(tPackage.id!!, version)?.metadata)
    }

    @Transactional(rollbackFor = [Throwable::class])
    override fun saveMetadata(request: PackageMetadataSaveRequest) {
        if (request.metadata.isNullOrEmpty()) {
            logger.info("Metadata key list is empty, skip saving[$this]")
            return
        }
        request.apply {
            val tPackage = checkPackage(projectId, repoName, packageKey)
            val tPackageVersion = checkPackageVersion(tPackage.id!!, version)
            val originalMetadata = MetadataUtils.toMap(tPackageVersion.metadata).toMutableMap()
            metadata!!.forEach { (key, value) -> originalMetadata[key] = value }
            tPackageVersion.metadata = MetadataUtils.fromMap(originalMetadata)
            packageVersionDao.save(tPackageVersion)
        }.also {
            logger.info("Save package metadata [$it] success.")
        }
    }

    @Transactional(rollbackFor = [Throwable::class])
    override fun deleteMetadata(request: PackageMetadataDeleteRequest) {
        if (request.keyList.isEmpty()) {
            logger.info("Metadata key list is empty, skip deleting[$this]")
            return
        }

        request.apply {
            val tPackage = checkPackage(projectId, repoName, packageKey)
            val query = PackageQueryHelper.versionQuery(tPackage.id!!, version)
            val update = Update().pull(
                TPackageVersion::metadata.name,
                Query.query(Criteria.where(TMetadata::key.name).`in`(keyList))
            )
            packageVersionDao.updateMulti(query, update)
        }.also {
            logger.info("Delete metadata [$it] success.")
        }
    }

    /**
     * 查找包，不存在则抛异常
     */
    private fun checkPackage(projectId: String, repoName: String, packageKey: String): TPackage {
        return packageDao.findByKey(projectId, repoName, packageKey)
            ?: throw ErrorCodeException(CommonMessageCode.RESOURCE_NOT_FOUND, packageKey)
    }

    /**
     * 查找版本，不存在则抛异常
     */
    private fun checkPackageVersion(packageId: String, versionName: String): TPackageVersion {
        return packageVersionDao.findByName(packageId, versionName)
            ?: throw ErrorCodeException(CommonMessageCode.RESOURCE_NOT_FOUND, versionName)
    }

    companion object {
        private val logger = LoggerFactory.getLogger(PackageMetadataServiceImpl::class.java)
    }
}
