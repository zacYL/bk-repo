package com.tencent.bkrepo.opdata.service.impl

import com.tencent.bkrepo.opdata.model.TVersionOp
import com.tencent.bkrepo.opdata.pojo.ArtifactMetricsData
import com.tencent.bkrepo.opdata.pojo.VersionOpUpdateRequest
import com.tencent.bkrepo.opdata.repository.VersionOpRepository
import com.tencent.bkrepo.opdata.service.VersionOpService
import org.springframework.data.domain.Sort
import org.springframework.data.mongodb.core.MongoTemplate
import org.springframework.data.mongodb.core.query.Criteria
import org.springframework.data.mongodb.core.query.Query
import org.springframework.data.mongodb.core.query.Update
import org.springframework.stereotype.Service
import java.time.LocalDateTime

@Service
class VersionOpServiceImpl(
    private val versionOpRepository: VersionOpRepository,
    private val mongoTemplate: MongoTemplate
) : VersionOpService {
    override fun update(request: VersionOpUpdateRequest): Boolean {
        val query = Query.query(
            Criteria.where(TVersionOp::projectId.name).`is`(request.projectId)
                .and(TVersionOp::repoName.name).`is`(request.repoName)
                .and(TVersionOp::packageKey.name).`is`(request.packageKey)
                .and(TVersionOp::packageVersion.name).`is`(request.packageVersion)
        )
        val update = Update().apply {
            this.set(TVersionOp::projectId.name, request.projectId)
            this.set(TVersionOp::repoName.name, request.repoName)
            this.set(TVersionOp::packageKey.name, request.packageKey)
            this.set(TVersionOp::type.name, request.type)
            this.set(TVersionOp::packageId.name, request.packageId)
            this.set(TVersionOp::packageName.name, request.packageName)
            this.set(TVersionOp::packageVersion.name, request.packageVersion)
            this.set(TVersionOp::downloads.name, request.downloads)
            this.set(TVersionOp::size.name, request.size)
            this.set(TVersionOp::lastModifiedDate.name, request.lastModifiedDate)
        }
        val result = mongoTemplate.upsert(query, update, TVersionOp::class.java)
        return result.modifiedCount == 1L
    }

    override fun getLatestModifiedTime(): LocalDateTime? {
        val records = mongoTemplate.findOne(
            Query().with(Sort.by(TVersionOp::lastModifiedDate.name).descending()).limit(1),
            TVersionOp::class.java
        ) ?: return null
        return records.lastModifiedDate
    }

    override fun versionSortByDownloads(projectId: String?, repoName: String?): List<ArtifactMetricsData> {
        val criteria = Criteria().apply {
            projectId?.let { this.and(TVersionOp::projectId.name).`is`(projectId) }
            repoName?.let { this.and(TVersionOp::repoName.name).`is`(repoName) }
        }
        val query = Query(criteria).with(Sort.by(TVersionOp::downloads.name).descending()).limit(DEFAULT_LIMIT)
        return mongoTemplate.find(query, TVersionOp::class.java).map { convert(it) }
    }

    private fun convert(tVersionOp: TVersionOp): ArtifactMetricsData {
        return ArtifactMetricsData(
            projectId = tVersionOp.projectId,
            repoName = tVersionOp.repoName,
            repoType = tVersionOp.type,
            packageKey = tVersionOp.packageKey,
            packageVersion = tVersionOp.packageVersion,
            packageName = tVersionOp.packageName,
            size = tVersionOp.size,
            count = tVersionOp.downloads
        )
    }

    companion object {
        const val DEFAULT_LIMIT = 10
    }
}
