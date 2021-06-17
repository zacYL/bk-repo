package com.tencent.bkrepo.opdata.pojo

import com.tencent.bkrepo.common.artifact.pojo.RepositoryType
import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty

@ApiModel("仓库类型数据")
data class RepoTypeData(
    @ApiModelProperty("仓库类型")
    val type: RepositoryType,
    @ApiModelProperty("仓库数量")
    var count: Long,
    @ApiModelProperty("仓库所占百分比")
    var percent: String
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as RepoTypeData

        if (type != other.type) return false

        return true
    }

    override fun hashCode(): Int {
        return type.hashCode()
    }
}

@ApiModel("仓库类型数据 列表")
data class RepoTypeSum(
    val repoTypeMetrics: Set<RepoTypeData?>
)
