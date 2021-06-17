package com.tencent.bkrepo.repository.pojo.metric

data class Metric(
    val id: String,
    var projectId: String?,
    var repoName: String?,
    var count: Long
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Metric

        if (id != other.id) return false

        return true
    }

    override fun hashCode(): Int {
        return id.hashCode()
    }
}
