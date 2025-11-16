package com.a307.linkcare.feature.caregroup.data

import android.os.Build
import androidx.annotation.RequiresApi
import com.a307.linkcare.feature.caregroup.MetricType
import com.a307.linkcare.feature.caregroup.domain.MemberSeries
import com.a307.linkcare.feature.caregroup.domain.WeeklyChartData
import com.a307.linkcare.feature.commongroup.domain.Member
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.time.temporal.TemporalAdjusters
import java.time.temporal.WeekFields
import java.util.Locale

@RequiresApi(Build.VERSION_CODES.O)
fun buildWeeklyChartData(
    type: MetricType,
    anchorDate: LocalDate,              // 선택된 날짜(해당 주)
    members: List<Member>
): WeeklyChartData {
    val monday = anchorDate.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
    val days = (0..6).map { monday.plusDays(it.toLong()) }
    val xLabels = listOf("월","화","수","목","금","토","일")

    // TODO: 실제 값으로 교체
    // 혈압: 하루 평균 혈압(수축기 기준 등) / 수면: 총 수면 분
    fun demoPointsForMember(idx: Int): List<Float> = when (type) {
        MetricType.BLOOD_PRESSURE -> days.map { d ->
            // 110~130 사이에서 멤버별/요일별 살짝 변동
            110f + (idx * 4) + (d.dayOfWeek.value % 5) * 2f
        }
        MetricType.SLEEP -> days.map { d ->
            // 360~480분(6~8시간) 범위 변동
            420f + ((idx - 1) * 10) + ((d.dayOfWeek.value % 3) * 15f)
        }
        else -> emptyList()
    }

    val memberSeries = members.mapIndexed { i, m ->
        MemberSeries(name = m.name, points = demoPointsForMember(i))
    }

    val avg = (0..6).map { day ->
        memberSeries.map { it.points[day] }.average().toFloat()
    }
    val avgSeries = MemberSeries(name = "평균", points = avg)

    return WeeklyChartData(xLabels = xLabels, series = memberSeries, avgSeries = avgSeries)
}

@RequiresApi(Build.VERSION_CODES.O)
val KOR_WEEK = WeekFields.of(DayOfWeek.SUNDAY, 1)
@RequiresApi(Build.VERSION_CODES.O)
val DATE_FMT = DateTimeFormatter.ofPattern("yyyy년 M월 d일 (E)", Locale.KOREA)

@RequiresApi(Build.VERSION_CODES.O)
 fun LocalDate.weekOfMonth(): Int = this.get(KOR_WEEK.weekOfMonth())
@RequiresApi(Build.VERSION_CODES.O)
 fun LocalDate.startOfWeek(): LocalDate = this.with(KOR_WEEK.dayOfWeek(), 1) // 일요일
@RequiresApi(Build.VERSION_CODES.O)
 fun LocalDate.endOfWeek(): LocalDate = this.with(KOR_WEEK.dayOfWeek(), 7)  // 토요일

@RequiresApi(Build.VERSION_CODES.O)
 fun LocalDate.korLabel(today: LocalDate = LocalDate.now()): String {
    val diff = ChronoUnit.DAYS.between(today, this).toInt()
    val suffix = when (diff) {
        0 -> " (오늘)"
        -1 -> " (어제)"
        1 -> " (내일)"
        else -> ""
    }
    return "${this.format(DATE_FMT)}$suffix"
}

@RequiresApi(Build.VERSION_CODES.O)
 fun LocalDate.toEpochMilliAtLocal(): Long =
    this.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()

@RequiresApi(Build.VERSION_CODES.O)
 fun epochMilliToLocalDate(millis: Long): LocalDate =
    Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()).toLocalDate()
