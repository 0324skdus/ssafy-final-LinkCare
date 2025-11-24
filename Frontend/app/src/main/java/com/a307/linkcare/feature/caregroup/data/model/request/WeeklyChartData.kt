package com.a307.linkcare.feature.caregroup.data.model.request

data class WeeklyChartData(
    val xLabels: List<String>,          // ["일","월","화","수","목","금","토"]
    val series: List<MemberSeries>,     // 멤버별 라인
    val avgSeries: MemberSeries         // 전체 평균 라인
)
