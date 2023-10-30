package com.tencent.bkrepo.auth.enums

enum class SeatBusiness(val desc: String) {
    CTEAM("CTeam"),
    CTEST("CTest"),
    CMEAS("CMeas"),
    CFLOW("CFlow"),
    ;

    companion object {
        val set = values().toSet()
    }
}

enum class SeatStatus(val value: Int, val desc: String) {
    DISABLE(0, "禁用"),
    ENABLE(1, "启用"),
    INVALID(2, "失效"),
    ;
    fun toInfo() = SeatStatusInfo(value, this, desc)
}

data class SeatStatusInfo(
    val value: Int,
    val status: SeatStatus,
    val desc: String
)

/**
 *  座席状态
 *  不包括禁用
 */
enum class SeatBusinessStatus(val business: SeatBusiness, val status: SeatStatus) {
    CTEAM_ENABLE(SeatBusiness.CTEAM, SeatStatus.ENABLE),
    CTEAM_INVALID(SeatBusiness.CTEAM, SeatStatus.INVALID),
    CTEST_ENABLE(SeatBusiness.CTEST, SeatStatus.ENABLE),
    CTEST_INVALID(SeatBusiness.CTEST, SeatStatus.INVALID),
    CMEAS_ENABLE(SeatBusiness.CMEAS, SeatStatus.ENABLE),
    CMEAS_INVALID(SeatBusiness.CMEAS, SeatStatus.INVALID),
    CFLOW_ENABLE(SeatBusiness.CFLOW, SeatStatus.ENABLE),
    CFLOW_INVALID(SeatBusiness.CFLOW, SeatStatus.INVALID),
    ;

    companion object {
        fun from(business: SeatBusiness, status: SeatStatus): SeatBusinessStatus? = values().find {
            it.business == business && it.status == status
        }

        fun from(
            cteamStatusInfo: SeatStatusInfo,
            ctestSeatStatusInfo: SeatStatusInfo,
            cmeasStatusInfo: SeatStatusInfo,
            cflowStatusInfo: SeatStatusInfo
        ): List<SeatBusinessStatus> {
            return mutableListOf<SeatBusinessStatus>().apply {
                from(SeatBusiness.CTEAM, cteamStatusInfo.status)?.let { add(it) }
                from(SeatBusiness.CTEST, ctestSeatStatusInfo.status)?.let { add(it) }
                from(SeatBusiness.CMEAS, cmeasStatusInfo.status)?.let { add(it) }
                from(SeatBusiness.CFLOW, cflowStatusInfo.status)?.let { add(it) }
            }
        }
    }
}
