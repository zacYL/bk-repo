package com.tencent.bkrepo.repository.pojo.storage

import com.tencent.bkrepo.common.api.constant.DEFAULT_PAGE_NUMBER
import com.tencent.bkrepo.common.api.constant.DEFAULT_PAGE_SIZE
import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty

@ApiModel("仓库存储分页查询参数")
data class RepoStorageInfoParam(
    @ApiModelProperty("当前页")
    val pageNumber: Int = DEFAULT_PAGE_NUMBER,
    @ApiModelProperty("分页大小")
    val pageSize: Int = DEFAULT_PAGE_SIZE,
    @ApiModelProperty("项目ID")
    val projectId: String?,
    @ApiModelProperty("顺序")
    val direction: DirectionType = DirectionType.REPO_SIZE_DESC
)
