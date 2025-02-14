package com.tencent.bkrepo.analyst.service.impl

import com.tencent.bkrepo.analyst.dao.VulRuleDao
import com.tencent.bkrepo.analyst.model.TVulRule
import com.tencent.bkrepo.analyst.pojo.request.VulRuleCreateRequest
import com.tencent.bkrepo.analyst.pojo.request.VulRuleDeleteRequest
import com.tencent.bkrepo.analyst.pojo.response.VulRuleDeleteResult
import com.tencent.bkrepo.analyst.pojo.response.VulRuleInfo
import com.tencent.bkrepo.analyst.service.VulRuleService
import com.tencent.bkrepo.common.api.constant.HttpStatus
import com.tencent.bkrepo.common.api.exception.ErrorCodeException
import com.tencent.bkrepo.common.api.message.CommonMessageCode
import com.tencent.bkrepo.common.api.pojo.Page
import com.tencent.bkrepo.common.api.util.EscapeUtils
import com.tencent.bkrepo.common.api.util.Preconditions
import com.tencent.bkrepo.common.artifact.event.base.EventType
import com.tencent.bkrepo.common.metadata.pojo.log.OperateLog
import com.tencent.bkrepo.common.metadata.service.log.OperateLogService
import com.tencent.bkrepo.common.mongo.dao.util.Pages
import com.tencent.bkrepo.common.security.util.SecurityUtils
import com.tencent.bkrepo.common.service.util.HttpContextHolder
import org.slf4j.LoggerFactory
import org.springframework.data.domain.Sort
import org.springframework.data.mongodb.core.query.Criteria
import org.springframework.data.mongodb.core.query.Query
import org.springframework.data.mongodb.core.query.inValues
import org.springframework.data.mongodb.core.query.isEqualTo
import org.springframework.data.mongodb.core.query.where
import org.springframework.stereotype.Service
import java.time.LocalDateTime

@Service
class VulRuleServiceImpl(
    private val vulRuleDao: VulRuleDao,
    private val operateLogService: OperateLogService
) : VulRuleService {

    override fun create(request: VulRuleCreateRequest) {
        Preconditions.checkArgument(request.vulRules.size in 1..200, "vulRules")
        val vulIdList = request.vulRules.map { it.vulId.toUpperCase() }
        Preconditions.checkArgument(vulIdList.toSet().size == vulIdList.size, "vulId")
        vulRuleDao.find(Query(where(TVulRule::vulId).inValues(vulIdList))).takeIf { it.isNotEmpty() }?.run {
            if (request.overwrite) {
                vulRuleDao.remove(Query(where(TVulRule::vulId).inValues(vulIdList)))
            } else throw ErrorCodeException(
                CommonMessageCode.RESOURCE_EXISTED, first().vulId, status = HttpStatus.CONFLICT
            )
        }
        val userId = SecurityUtils.getUserId()
        val now = LocalDateTime.now()
        vulRuleDao.insert(
            request.vulRules.map {
                TVulRule(
                    vulId = it.vulId.toUpperCase(),
                    pass = it.pass,
                    description = it.description,
                    createdBy = userId,
                    createdDate = now
                )
            }
        )
        if (request.vulRules.size == 1) operateLog(EventType.VUL_RULE_ADD, mapOf("vulId" to vulIdList.first()))
        else if (request.vulRules.size > 1) operateLog(EventType.VUL_RULE_ADD_BATCH, mapOf("vulIds" to vulIdList))
        logger.info("success create rule of $vulIdList by user[$userId]")
    }

    private fun operateLog(type: EventType, data: Map<String, Any>, date: LocalDateTime = LocalDateTime.now()) {
        val log = OperateLog(
            createdDate = date,
            type = type.name,
            projectId = "",
            repoName = "",
            resourceKey = "",
            userId = SecurityUtils.getUserId(),
            clientAddress = HttpContextHolder.getClientAddress(),
            description = data
        )
        operateLogService.saveAsync(log)
    }

    override fun getByVulId(vulId: String, pass: Boolean?): VulRuleInfo {
        return vulRuleDao.getByVulId(vulId.toUpperCase(), pass)?.let {
            transformToInfo(it)
        } ?: throw ErrorCodeException(CommonMessageCode.RESOURCE_NOT_FOUND, vulId, status = HttpStatus.NOT_FOUND)
    }

    override fun listByVulIds(vulIds: List<String>): List<VulRuleInfo> {
        val query = Query(Criteria.where(TVulRule::vulId.name).`in`(vulIds))
        val result = vulRuleDao.find(query)
        return result.map { transformToInfo(it) }
    }

    override fun getVulList(): List<VulRuleInfo> {
        val result = vulRuleDao.findAll()
        return result.map { transformToInfo(it) }
    }

    override fun delete(request: VulRuleDeleteRequest): VulRuleDeleteResult {
        val vulIds = request.vulIdList.map { it.toUpperCase() }.toSet()
        logger.info("delete vul rule $vulIds by user [${SecurityUtils.getUserId()}]")
        val result = vulRuleDao.deleteByVulId(vulIds)
        if (vulIds.size == 1) operateLog(EventType.VUL_RULE_REMOVE, mapOf("vulId" to vulIds.first()))
        else if (vulIds.size > 1) operateLog(EventType.VUL_RULE_REMOVE_BATCH, mapOf("vulIds" to vulIds))
        return VulRuleDeleteResult(result.deletedCount)
    }

    override fun pageByVulId(pageNumber: Int, pageSize: Int, vulId: String?, pass: Boolean?): Page<VulRuleInfo> {
        Preconditions.checkArgument(pageNumber >= 0, "pageNumber must be greater than or equal to 0")
        Preconditions.checkArgument(pageSize >= 0, "pageSize must be greater than or equal to 0")
        val pageRequest = Pages.ofRequest(pageNumber, pageSize)
        val criteria = Criteria()
        if (!vulId.isNullOrBlank()) {
            criteria.and(TVulRule::vulId.name).regex(EscapeUtils.escapeRegex(vulId), "i")
        }
        if (pass != null) {
            criteria.and(TVulRule::pass.name).isEqualTo(pass)
        }
        val query = Query(criteria)
        val totalRecord = vulRuleDao.count(query)
        val page = vulRuleDao.find(query.with(pageRequest).with(Sort.by(TVulRule::createdDate.name).descending()))
            .map { transformToInfo(it) }.toList()
        return Pages.ofResponse(pageRequest, totalRecord, page)
    }

    private fun transformToInfo(vulRule: TVulRule): VulRuleInfo {
        return VulRuleInfo(
            vulId = vulRule.vulId,
            pass = vulRule.pass,
            description = vulRule.description,
            createdBy = vulRule.createdBy,
            createdDate = vulRule.createdDate
        )
    }

    companion object {
        private val logger = LoggerFactory.getLogger(VulRuleServiceImpl::class.java)
    }
}
