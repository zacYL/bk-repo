package com.tencent.bkrepo.scanner.service.impl

import com.tencent.bkrepo.common.api.pojo.Page
import com.tencent.bkrepo.common.api.util.JsonUtils
import com.tencent.bkrepo.common.query.model.PageLimit
import com.tencent.bkrepo.common.security.util.SecurityUtils
import com.tencent.bkrepo.scanner.dao.SpdxLicenseDao
import com.tencent.bkrepo.scanner.exception.LicenseNotFoundException
import com.tencent.bkrepo.scanner.model.TSpdxLicense
import com.tencent.bkrepo.scanner.pojo.license.SpdxLicenseInfo
import com.tencent.bkrepo.scanner.pojo.license.SpdxLicenseJsonInfo
import com.tencent.bkrepo.scanner.pojo.license.UpdateLicenseRequest
import com.tencent.bkrepo.scanner.service.SpdxLicenseService
import org.slf4j.LoggerFactory
import org.springframework.data.mongodb.core.MongoTemplate
import org.springframework.data.mongodb.core.query.Criteria
import org.springframework.data.mongodb.core.query.Query
import org.springframework.stereotype.Service
import java.io.File
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@Service
class SpdxLicenseServiceImpl(
    private val licenseDao: SpdxLicenseDao,
    private val mongoTemplate: MongoTemplate
) : SpdxLicenseService {
    override fun importLicense(path: String): Boolean {
        val operator = SecurityUtils.getUserId()
        logger.info("userId:$operator, import license data")
        //TODO try catch
        val licenseJsonInfo =
            JsonUtils.objectMapper.readValue(File("D:\\xxx\\licenses.json"), SpdxLicenseJsonInfo::class.java)
        val licenses = licenseJsonInfo.licenses
        val licenseList = mutableListOf<TSpdxLicense>()
        licenses.forEach {
            val result = licenseDao.findByLicenseId(it.licenseId)
            if (result == null) {
                val license = TSpdxLicense(
                    createdBy = operator,
                    createdDate = LocalDateTime.now(),
                    lastModifiedBy = operator,
                    lastModifiedDate = LocalDateTime.now(),
                    name = it.name,
                    licenseId = it.licenseId,
                    seeAlso = it.seeAlso,
                    reference = it.reference,
                    isDeprecatedLicenseId = it.isDeprecatedLicenseId,
                    isOsiApproved = it.isOsiApproved,
                    isFsfLibre = it.isFsfLibre,
                    detailsUrl = it.detailsUrl
                )
                licenseList.add(license)
            } else {
                val license = TSpdxLicense(
                    createdBy = operator,
                    createdDate = LocalDateTime.now(),
                    lastModifiedBy = operator,
                    lastModifiedDate = LocalDateTime.now(),
                    name = it.name,
                    licenseId = it.licenseId,
                    seeAlso = it.seeAlso,
                    reference = it.reference,
                    isDeprecatedLicenseId = it.isDeprecatedLicenseId,
                    isOsiApproved = it.isOsiApproved,
                    isFsfLibre = it.isFsfLibre,
                    detailsUrl = it.detailsUrl,
                    isTrust = result.isTrust,
                    risk = result.risk
                )
                licenseList.add(license)
            }
        }
        licenseDao.insert(licenseList)
        return true
    }

    override fun listLicensePage(
        name: String?,
        isDeprecatedLicenseId: Boolean?,
        isTrust: Boolean?,
        pageLimit: PageLimit
    ): Page<SpdxLicenseInfo> {
        val page = licenseDao.page(name, isDeprecatedLicenseId, isTrust, pageLimit)
        val licenseList = page.records.map { convert(it)!! }
        return Page(page.pageNumber, page.pageSize, page.totalRecords, licenseList)
    }


    override fun listLicense(): List<SpdxLicenseInfo> {
        return licenseDao.findAll().map { convert(it)!! }
    }

    override fun getLicenseInfo(licenseId: String): SpdxLicenseInfo? {
        return convert(licenseDao.findByLicenseId(licenseId))
    }

    override fun updateLicense(licenseId: String, request: UpdateLicenseRequest): Boolean {
        logger.info("update license by [${SecurityUtils.getUserId()}] ,request: [$request]")
        val tLicense = checkLicenseExist(licenseId)
        request.isTrust?.let {
            tLicense.isTrust = it
        }
        request.risk?.let {
            tLicense.risk = it
        }
        licenseDao.save(tLicense)
        return true
    }

    override fun listLicenseByIds(licenseIds: List<String>): Map<String, SpdxLicenseInfo> {
        val licenseMap = mutableMapOf<String, SpdxLicenseInfo>()
        val licenseList = mongoTemplate.find(
            Query.query(
                Criteria.where(TSpdxLicense::licenseId.name)
                    .`in`(licenseIds)
            ), TSpdxLicense::class.java
        ).map { convert(it) }
        licenseList.forEach {
            if (it != null) {
                licenseMap[it.licenseId] = it
            }
        }
        return licenseMap
    }

    private fun checkLicenseExist(licenseId: String): TSpdxLicense {
        val license = licenseDao.findByLicenseId(licenseId)
        license ?: run {
            logger.warn("license [$licenseId] not exist")
            throw LicenseNotFoundException(licenseId)
        }
        return license
    }


    companion object {
        private val logger = LoggerFactory.getLogger(SpdxLicenseServiceImpl::class.java)

        private fun convert(tSpdxLicense: TSpdxLicense?): SpdxLicenseInfo? {
            return tSpdxLicense?.let {
                SpdxLicenseInfo(
                    createdBy = it.createdBy,
                    createdDate = it.createdDate.format(DateTimeFormatter.ISO_DATE_TIME),
                    lastModifiedBy = it.lastModifiedBy,
                    lastModifiedDate = it.lastModifiedDate.format(DateTimeFormatter.ISO_DATE_TIME),
                    name = it.name,
                    licenseId = it.licenseId,
                    seeAlso = it.seeAlso,
                    reference = it.reference,
                    isDeprecatedLicenseId = it.isDeprecatedLicenseId,
                    isOsiApproved = it.isOsiApproved,
                    isFsfLibre = it.isFsfLibre,
                    detailsUrl = it.detailsUrl,
                    isTrust = it.isTrust,
                    risk = it.risk
                )
            }
        }
    }
}
