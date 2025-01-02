package com.tencent.bkrepo.analyst.service

import com.tencent.bkrepo.analyst.pojo.request.VulRuleCreateRequest
import com.tencent.bkrepo.analyst.pojo.request.VulRuleDeleteRequest
import com.tencent.bkrepo.analyst.pojo.response.VulRuleDeleteResult
import com.tencent.bkrepo.analyst.pojo.response.VulRuleInfo
import com.tencent.bkrepo.common.api.pojo.Page

interface VulRuleService {
    fun create(request: VulRuleCreateRequest)

    fun getByVulId(vulId: String, pass: Boolean?): VulRuleInfo

    fun listByVulIds(vulIds: List<String>): List<VulRuleInfo>

    fun getVulList(): List<VulRuleInfo>

    fun delete(request: VulRuleDeleteRequest): VulRuleDeleteResult

    fun pageByVulId(pageNumber: Int, pageSize: Int, vulId: String?, pass: Boolean?): Page<VulRuleInfo>
}
