package com.ssafy.sdk.health.domain.backup.sync.common

sealed class Windowing {
    data object Single : Windowing()
    data class ByYears(val yearsPerChunk: Long) : Windowing()
//    data class ByMonths(val monthsPerChunk: Long) : Windowing()
}
