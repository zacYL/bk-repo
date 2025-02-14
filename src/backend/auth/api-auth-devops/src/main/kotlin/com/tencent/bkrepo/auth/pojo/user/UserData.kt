package com.tencent.bkrepo.auth.pojo.user

import com.fasterxml.jackson.annotation.JsonProperty
import com.tencent.bkrepo.auth.enums.SeatBusinessStatus
import com.tencent.bkrepo.auth.enums.SeatStatus
import com.tencent.bkrepo.auth.enums.SeatStatusInfo
import io.swagger.annotations.ApiModelProperty

data class UserData(
    var id: String,
    var displayName: String,
    var passWord: String? = "",
    var userEncrypt: String? = "",
    var email: String? = "",
    var weChatId: String? = "",
    var cteamSeat: SeatStatusInfo = SeatStatus.DISABLE.toInfo(),
    var ctestSeat: SeatStatusInfo = SeatStatus.DISABLE.toInfo(),
    var cmeasSeat: SeatStatusInfo = SeatStatus.DISABLE.toInfo(),
    var cflowSeat: SeatStatusInfo = SeatStatus.DISABLE.toInfo()
) {
    @JsonProperty("seatStatus")
    @Suppress("unused")
    @ApiModelProperty("座席状态, 包含启用和失效, 不包含禁用")
    val seatStatus: List<SeatBusinessStatus> = SeatBusinessStatus.from(cteamSeat, ctestSeat, cmeasSeat, cflowSeat)
}
