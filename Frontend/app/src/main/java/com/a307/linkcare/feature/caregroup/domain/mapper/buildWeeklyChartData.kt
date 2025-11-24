package com.a307.linkcare.feature.caregroup.domain.mapper

import android.os.Build
import androidx.annotation.RequiresApi
import com.a307.linkcare.feature.caregroup.ui.home.MetricType
import com.a307.linkcare.feature.caregroup.data.model.request.MemberSeries
import com.a307.linkcare.feature.caregroup.data.model.response.SleepStatisticsResponse
import com.a307.linkcare.feature.caregroup.data.model.request.WeeklyChartData
import com.a307.linkcare.feature.commongroup.domain.model.Member
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
    anchorDate: LocalDate,
    members: List<Member>,
    sleepStats: SleepStatisticsResponse? = null
): WeeklyChartData {

    val monday = anchorDate.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
    val days = (0..6).map { monday.plusDays(it.toLong()) }
    val xLabels = listOf("월","화","수","목","금","토","일")

    /* --------------------
       1) 혈압: 기존 데모 데이터 유지
       -------------------- */
    fun demoBp(idx: Int): List<Float> =
        days.map { d ->
            110f + (idx * 4) + (d.dayOfWeek.value % 5) * 2f
        }

    /* --------------------
       2) 수면: dailySleepMinutes(ms) → minutes 변환
       -------------------- */

    val memberSeries: List<MemberSeries> = when (type) {

        MetricType.BLOOD_PRESSURE -> {
            members.mapIndexed { i, m ->
                MemberSeries(name = m.name, points = demoBp(i))
            }
        }

        MetricType.SLEEP -> {
            if (sleepStats != null) {

                val statsByUser = sleepStats.members.associateBy { it.userSeq }

                members.mapNotNull { member ->
                    val stat = statsByUser[member.userPk] ?: return@mapNotNull null

                    val minutesList: List<Float> = stat.dailySleepMinutes.map { raw ->
                        (raw / 60000f).coerceAtLeast(0f)
                    }

                    val normalized = when {
                        minutesList.size == 7 -> minutesList
                        minutesList.size > 7 -> minutesList.take(7)
                        minutesList.isEmpty() -> List(7) { 0f }
                        else -> {
                            val d = 7 - minutesList.size
                            minutesList + List(d) { 0f }
                        }
                    }

                    MemberSeries(
                        name = member.name,
                        points = normalized
                    )
                }

            } else emptyList()
        }

        else -> emptyList()
    }

    /* --------------------
       3) 평균 라인 계산
       -------------------- */
    val avgSeries: MemberSeries = when {
        type == MetricType.SLEEP -> {
            val pointCount = memberSeries.firstOrNull()?.points?.size ?: 7
            val targetMinutes = 7 * 60f
            MemberSeries(
                name = "목표 7시간",
                points = List(pointCount) { targetMinutes }
            )
        }
        memberSeries.isNotEmpty() -> {
            val pointCount = memberSeries.first().points.size
            val avgPoints = (0 until pointCount).map { idx ->
                memberSeries.map { it.points[idx] }.average().toFloat()
            }
            MemberSeries(name = "평균", points = avgPoints)
        }

        else -> MemberSeries(name = "평균", points = emptyList())
    }


    return WeeklyChartData(
        xLabels = xLabels,
        series = memberSeries,
        avgSeries = avgSeries
    )
}

@RequiresApi(Build.VERSION_CODES.O)
val KOR_WEEK = WeekFields.of(DayOfWeek.MONDAY, 1)
@RequiresApi(Build.VERSION_CODES.O)
val DATE_FMT = DateTimeFormatter.ofPattern("yyyy년 M월 d일 (E)", Locale.KOREA)

@RequiresApi(Build.VERSION_CODES.O)
fun LocalDate.weekOfMonth(): Int = this.get(KOR_WEEK.weekOfMonth())

@RequiresApi(Build.VERSION_CODES.O)
 fun LocalDate.startOfWeek(): LocalDate = this.with(KOR_WEEK.dayOfWeek(), 1) // 월요일
@RequiresApi(Build.VERSION_CODES.O)
 fun LocalDate.endOfWeek(): LocalDate = this.with(KOR_WEEK.dayOfWeek(), 7)  // 일요일

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
