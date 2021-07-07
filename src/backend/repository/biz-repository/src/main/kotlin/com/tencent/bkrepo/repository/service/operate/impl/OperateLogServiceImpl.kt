package com.tencent.bkrepo.repository.service.operate.impl

import com.tencent.bkrepo.common.api.pojo.Page
import com.tencent.bkrepo.common.mongo.dao.util.Pages
import com.tencent.bkrepo.repository.BK_SOFTWARE
import com.tencent.bkrepo.repository.dao.OperateLogDao
import com.tencent.bkrepo.repository.model.TOperateLog
import com.tencent.bkrepo.repository.pojo.log.ResourceType
import com.tencent.bkrepo.repository.pojo.operate.OperateLogResponse
import com.tencent.bkrepo.repository.service.operate.OperateLogService
import com.tencent.bkrepo.repository.util.OperateMapUtils
import org.springframework.data.domain.Sort
import org.springframework.data.mongodb.core.query.Criteria
import org.springframework.data.mongodb.core.query.Query
import org.springframework.stereotype.Service
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@Service
class OperateLogServiceImpl(
    private val operateLogDao: OperateLogDao
) : OperateLogService {
    override fun page(
        type: ResourceType?,
        projectId: String?,
        repoName: String?,
        operator: String?,
        startTime: String?,
        endTime: String?,
        pageNumber: Int,
        pageSize: Int
    ): Page<OperateLogResponse> {
        val criteria = if (type != null) {
            Criteria.where(TOperateLog::resourceType.name).`is`(type)
        } else {
            Criteria.where(TOperateLog::resourceType.name).ne(ResourceType.NODE)
        }

        if (projectId == null) {
            criteria.and("${TOperateLog::description.name}.projectId").`in`(BK_SOFTWARE, null)
        } else {
            criteria.and("${TOperateLog::description.name}.projectId").`in`(projectId, null)
        }

        repoName?.let { criteria.and("${TOperateLog::description.name}.repoName").`is`(repoName) }

        operator?.let { criteria.and(TOperateLog::userId.name).`is`(operator) }
        val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSz")
        startTime?.let {
            val localDateTime = LocalDateTime.parse(it, formatter)
            criteria.and(TOperateLog::createdDate.name).gte(localDateTime)
        }

        endTime?.let {
            val localDateTime = LocalDateTime.parse(it, formatter)
            criteria.and(TOperateLog::createdDate.name).lte(localDateTime)
        }

        val pageRequest = Pages.ofRequest(pageNumber, pageSize)
        val query = Query(criteria).with(Sort.by(TOperateLog::createdDate.name).descending())
        val totalRecords = operateLogDao.count(query)
        val records = operateLogDao.find(query.with(pageRequest))
            .map { convert(it) }

        return Pages.ofResponse(pageRequest, totalRecords, records)
    }

    private fun convert(tOperateLog: TOperateLog): OperateLogResponse {
        val content = when (tOperateLog.resourceType) {
            ResourceType.PACKAGE -> {
                val projectId = tOperateLog.description["projectId"] as? String
                val repoName = tOperateLog.description["repoName"] as? String
                val packageName = tOperateLog.description["packageName"] as? String
                val version = tOperateLog.description["packageVersion"] as? String
                val repoType = tOperateLog.description["repoType"] as? String
                OperateLogResponse.Content(
                    projectId = projectId,
                    repoType = repoType,
                    resKey = "$repoName::$packageName::$version"
                )
            }
            ResourceType.REPOSITORY -> {
                val request = tOperateLog.description["request"] as? Map<String, Any?>
                val repoType = tOperateLog.description["repoType"] as? String
                OperateLogResponse.Content(
                    projectId = request!!["projectId"] as? String,
                    repoType = repoType,
                    resKey = "${request["name"] as? String}"
                )
            }
            ResourceType.ADMIN -> {
                val list = tOperateLog.description["list"] as? List<String>
                OperateLogResponse.Content(
                    resKey = list!!.joinToString("::")
                )
            }
            else -> OperateLogResponse.Content(
                resKey = ""
            )
        }

        return OperateLogResponse(
            createdDate = tOperateLog.createdDate,
            resourceType = tOperateLog.resourceType.nick,
            operateType = OperateMapUtils.transferOperationType(
                tOperateLog.resourceType, tOperateLog.operateType
            ),
            userId = tOperateLog.userId,
            clientAddress = tOperateLog.clientAddress,
            result = true,
            content = content
        )
    }
}
