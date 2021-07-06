package com.tencent.bkrepo.auth.pojo.software

data class SoftwareUseUnit(
    val unitId: String,
    val unitType: UnitType,
    val allowPush: Boolean
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (SoftwareUseUnit::javaClass != other?.javaClass) return false

        other as SoftwareUseUnit

        if (unitId != other.unitId) return false

        return true
    }

    override fun hashCode(): Int {
        return unitId.hashCode()
    }
}
