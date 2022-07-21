/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
 *
 * Copyright (C) 2022 THL A29 Limited, a Tencent company.  All rights reserved.
 *
 * BK-CI 蓝鲸持续集成平台 is licensed under the MIT license.
 *
 * A copy of the MIT License is included in this file.
 *
 *
 * Terms of the MIT License:
 * ---------------------------------------------------
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
 * documentation files (the "Software"), to deal in the Software without restriction, including without limitation the
 * rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to
 * permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of
 * the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT
 * LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN
 * NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
 * SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package com.tencent.bkrepo.scanner.event.listener

import com.tencent.bkrepo.common.artifact.constant.FORBID_STATUS
import com.tencent.bkrepo.common.artifact.constant.FORBID_TYPE
import com.tencent.bkrepo.common.artifact.constant.SCAN_STATUS
import com.tencent.bkrepo.common.artifact.pojo.RepositoryType
import com.tencent.bkrepo.repository.api.MetadataClient
import com.tencent.bkrepo.repository.api.PackageMetadataClient
import com.tencent.bkrepo.repository.pojo.metadata.ForbidType
import com.tencent.bkrepo.repository.pojo.metadata.MetadataModel
import com.tencent.bkrepo.repository.pojo.metadata.MetadataSaveRequest
import com.tencent.bkrepo.repository.pojo.metadata.packages.PackageMetadataSaveRequest
import com.tencent.bkrepo.scanner.event.SubtaskStatusChangedEvent
import com.tencent.bkrepo.scanner.model.SubScanTaskDefinition
import com.tencent.bkrepo.scanner.model.TPlanArtifactLatestSubScanTask
import com.tencent.bkrepo.scanner.utils.ScanPlanConverter
import org.slf4j.LoggerFactory
import org.springframework.context.event.EventListener
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Component

@Component
class SubtaskStatusChangedEventListener(
    private val metadataClient: MetadataClient,
    private val packageMetadataClient: PackageMetadataClient
) {
    @Async
    @EventListener(SubtaskStatusChangedEvent::class)
    fun listen(event: SubtaskStatusChangedEvent) {
        with(event.subtask) {
            // 未指定扫描方案表示为系统级别触发的扫描，不更新元数据
            if (planId == null) {
                return
            }

            // 更新扫描状态元数据
            val metadata = ArrayList<MetadataModel>(4)
            metadata.add(
                MetadataModel(
                    key = SCAN_STATUS,
                    value = ScanPlanConverter.convertToScanStatus(status).name,
                    system = true
                )
            )
            // 更新质量规则元数据
            qualityRedLine?.let {
                // 未通过质量规则，判断是否触发禁用
                if (!qualityRedLine) {
                    addForbidMetadata(this, metadata)
                }
                metadata.add(MetadataModel(key = SubScanTaskDefinition::qualityRedLine.name, value = it, system = true))
            }
            if (repoType == RepositoryType.GENERIC.name) {
                val request = MetadataSaveRequest(
                    projectId = projectId,
                    repoName = repoName,
                    fullPath = fullPath,
                    nodeMetadata = metadata
                )
                metadataClient.saveMetadata(request)
            } else {
                val request = PackageMetadataSaveRequest(
                    projectId = projectId,
                    repoName = repoName,
                    packageKey = packageKey!!,
                    version = version!!,
                    versionMetadata = metadata
                )
                packageMetadataClient.saveMetadata(request)
            }
            logger.info("update project[$projectId] repo[$repoName] fullPath[$fullPath] metadata[$metadata] success")
        }
    }

    /**
     * 如果方案设置forbidQualityUnPass=true，保存禁用信息
     * 保存metadata(forbidStatus禁用状态(true)、forbidType禁用类型(qualityUnPass))
     */
    fun addForbidMetadata(subTask: TPlanArtifactLatestSubScanTask, metadata: ArrayList<MetadataModel>) {
        with(subTask) {
            // 方案禁用触发设置
            val forbidQualityUnPass = scanQuality?.get(FORBID_QUALITY_UNPASS) as Boolean?
            if (forbidQualityUnPass == true) {
                metadata.add(
                    MetadataModel(
                        key = FORBID_STATUS,
                        value = true,
                        system = true
                    )
                )
                metadata.add(
                    MetadataModel(
                        key = FORBID_TYPE,
                        value = ForbidType.QUALITY_UNPASS.name,
                        system = true
                    )
                )
            }
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(SubtaskStatusChangedEventListener::class.java)

        // 禁用质量规则未通过的制品
        const val FORBID_QUALITY_UNPASS = "forbidQualityUnPass"
    }
}
