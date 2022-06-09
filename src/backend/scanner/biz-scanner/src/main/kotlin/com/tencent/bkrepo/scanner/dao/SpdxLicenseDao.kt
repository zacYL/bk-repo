package com.tencent.bkrepo.scanner.dao

import com.tencent.bkrepo.common.api.pojo.Page
import com.tencent.bkrepo.common.mongo.dao.simple.SimpleMongoDao
import com.tencent.bkrepo.common.mongo.dao.util.Pages
import com.tencent.bkrepo.common.query.model.PageLimit
import com.tencent.bkrepo.scanner.model.TSpdxLicense
import org.springframework.data.domain.Sort
import org.springframework.data.mongodb.core.query.Criteria
import org.springframework.data.mongodb.core.query.Query
import org.springframework.data.mongodb.core.query.isEqualTo
import org.springframework.stereotype.Repository

@Repository
class SpdxLicenseDao : SimpleMongoDao<TSpdxLicense>() {
    fun findByLicenseId(licenseId: String): TSpdxLicense? {
        return this.findOne(Query(TSpdxLicense::licenseId.isEqualTo(licenseId)))
    }

    fun page(
        name:String?,
        isDeprecatedLicenseId: Boolean?,
        isTrust: Boolean?,
        pageLimit: PageLimit
    ): Page<TSpdxLicense>{
        val criteria = Criteria()
        name?.let {
            criteria.orOperator(
                Criteria.where(TSpdxLicense::name.name).regex(".*$name.*"),
                Criteria.where(TSpdxLicense::licenseId.name).regex(".*$name.*")
            )
        }
        isDeprecatedLicenseId?.let { criteria.and(TSpdxLicense::isDeprecatedLicenseId.name).`is`(it) }
        isTrust?.let { criteria.and(TSpdxLicense::isTrust.name).`is`(it) }
        val pageRequest = Pages.ofRequest(pageLimit.getNormalizedPageNumber(), pageLimit.getNormalizedPageSize())
        val query = Query(criteria).with(pageRequest).with(Sort.by(TSpdxLicense::createdDate.name).descending())
        return Pages.ofResponse(pageRequest, count(query), find(query))
    }
}
