package com.a307.linkcare.common.component.molecules.care

import android.graphics.Paint
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.a307.linkcare.common.theme.black
import com.a307.linkcare.feature.caregroup.data.model.request.MemberSeries
import com.a307.linkcare.feature.caregroup.data.model.request.WeeklyChartData

@Composable
fun MetricChartCard(
    title: String,
    unit: String,
    data: WeeklyChartData,
    modifier: Modifier = Modifier
) {
    val palette = listOf(
        Color(0xFF4A89F6), // 진한 블루
        Color(0xFF00BFA6), // 민트
        Color(0xFFFF6B35), // 오렌지
        Color(0xFF9C27B0), // 보라
        Color(0xFF009688), // 청록
        Color(0xFFEF5350)  // 레드
    )
    val displayedSeries = remember(data.series) { data.series.take(6) }

    val seriesColors = remember(displayedSeries.size, palette) {
        List(displayedSeries.size) { i -> palette[i % palette.size] }
    }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(12.dp),
        tonalElevation = 1.dp
    ) {
        Column(Modifier.padding(16.dp)) {
            // 타이틀 + 단위 배지
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(title, style = MaterialTheme.typography.titleMedium, color = black)
                Spacer(Modifier.width(8.dp))
                UnitBadge(unit = unit)
            }
            Spacer(Modifier.height(8.dp))

            LegendRow(series = displayedSeries, colors = seriesColors)

            Spacer(Modifier.height(8.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(220.dp)
            ) {
                Canvas(modifier = Modifier.matchParentSize()) {
                    val paddingLeft = 44f
                    val paddingRight = 12f
                    val paddingTop = 12f
                    val paddingBottom = 28f

                    val w = size.width - paddingLeft - paddingRight
                    val h = size.height - paddingTop - paddingBottom

                    val isSleep = (unit == "분")

                    // 모든 값 모으기 (멤버 시리즈 + 평균 시리즈)
                    val allValuesRaw = displayedSeries.flatMap { it.points } + data.avgSeries.points
                    val allValues = if (allValuesRaw.isEmpty()) listOf(0f) else allValuesRaw

                    // ---- Y축 범위 / 눈금 계산 ----
                    val yMin: Float
                    val yMax: Float
                    val ySteps: Int
                    val valueRange: Float

                    if (isSleep) {
                        // 수면 그래프: 분 단위 값 → 최대 수면 시간 기준으로 동적 스케일
                        val maxSleepMinutes = (allValues.maxOrNull() ?: 0f)
                        // 최대 시간(정수) + 1시간 버퍼, 최소 6시간은 보이게
                        val maxHours = ((maxSleepMinutes / 60f).toInt() + 1).coerceAtLeast(6)
                        yMin = 0f
                        yMax = (maxHours * 60).toFloat()   // 분 단위
                        ySteps = maxHours                  // 1시간 = 60분 간격
                        valueRange = (yMax - yMin).takeIf { it > 0f } ?: 1f

                        // Y축 라인 + 라벨 (0시간, 1시간, 2시간, ...)
                        repeat(ySteps + 1) { i ->
                            val y = paddingTop + h - (h / ySteps) * i
                            val minutesValue = yMin + 60f * i
                            val hourText = "${(minutesValue / 60f).toInt()}시간"

                            // 가로 보조선
                            drawLine(
                                color = Color(0x11000000),
                                start = Offset(paddingLeft, y),
                                end = Offset(size.width - paddingRight, y),
                                strokeWidth = 1f
                            )

                            // Y축 라벨
                            drawContext.canvas.nativeCanvas.drawText(
                                hourText,
                                paddingLeft - 8f,
                                y + 6f,
                                Paint().apply {
                                    color = 0xFF666666.toInt()
                                    textSize = 28f
                                    textAlign = Paint.Align.RIGHT
                                }
                            )
                        }
                    } else {
                        // 기존 로직: min/max 기반 4분할
                        val minV = (allValues.minOrNull() ?: 0f)
                        val maxV = (allValues.maxOrNull() ?: 1f)
                        val range = (maxV - minV).takeIf { it > 0f } ?: 1f
                        val steps = 4

                        yMin = minV
                        yMax = maxV
                        ySteps = steps
                        valueRange = range

                        repeat(steps + 1) { i ->
                            val y = paddingTop + h - (h / steps) * i
                            val v = minV + range / steps * i

                            drawLine(
                                color = Color(0x11000000),
                                start = Offset(paddingLeft, y),
                                end = Offset(size.width - paddingRight, y),
                                strokeWidth = 1f
                            )
                            drawContext.canvas.nativeCanvas.drawText(
                                v.toInt().toString(),
                                paddingLeft - 4f,
                                y + 4f,
                                Paint().apply {
                                    color = 0xFF666666.toInt()
                                    textSize = 28f
                                    textAlign = Paint.Align.RIGHT
                                }
                            )
                        }
                    }

                    // X축 (data.xLabels가 월~일 순서라고 가정)
                    val xStep = w / (data.xLabels.size - 1).coerceAtLeast(1)
                    data.xLabels.forEachIndexed { i, label ->
                        val x = paddingLeft + i * xStep
                        val y = size.height - paddingBottom / 2f
                        drawContext.canvas.nativeCanvas.drawText(
                            label,
                            x,
                            y + 20f,
                            Paint().apply {
                                color = 0xFF666666.toInt()
                                textSize = 28f
                                textAlign = Paint.Align.CENTER
                            }
                        )
                    }

                    fun pt(i: Int, v: Float): Offset {
                        val x = paddingLeft + i * xStep
                        val range = if (valueRange > 0f) valueRange else 1f
                        val ratio = (v - yMin) / range
                        val y = paddingTop + h - (ratio * h)
                        return Offset(x, y)
                    }

                    // 시리즈 라인
                    displayedSeries.forEachIndexed { idx, s ->
                        if (s.points.isEmpty()) return@forEachIndexed
                        val path = Path().apply {
                            s.points.forEachIndexed { i, v ->
                                val p = pt(i, v)
                                if (i == 0) moveTo(p.x, p.y) else lineTo(p.x, p.y)
                            }
                        }
                        drawPath(
                            path = path,
                            color = seriesColors[idx],
                            style = Stroke(width = 4f, cap = StrokeCap.Round, join = StrokeJoin.Round)
                        )
                    }

                    // 평균 / 목표 라인 (점선)
                    if (data.avgSeries.points.isNotEmpty()) {
                        val avgPath = Path().apply {
                            data.avgSeries.points.forEachIndexed { i, v ->
                                val p = pt(i, v)
                                if (i == 0) moveTo(p.x, p.y) else lineTo(p.x, p.y)
                            }
                        }
                        drawPath(
                            path = avgPath,
                            color = Color.Black.copy(alpha = 0.6f),
                            style = Stroke(
                                width = 5f,
                                pathEffect = PathEffect.dashPathEffect(floatArrayOf(14f, 10f), 0f),
                                cap = StrokeCap.Round,
                                join = StrokeJoin.Round
                            )
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun UnitBadge(unit: String) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = Color(0xFFF1F3F7)
    ) {
        Text(
            text = unit,
            fontSize = 12.sp,
            color = Color(0xFF444444),
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}

@Composable
private fun LegendRow(
    series: List<MemberSeries>,
    colors: List<Color>
) {
    Column {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            series.forEachIndexed { idx, s ->
                LegendDot(
                    label = s.name,
                    color = colors[idx % colors.size]
                )
            }
        }
        Spacer(Modifier.height(4.dp))
        // avgSeries.name이 "목표 7시간" 같은 이름이면 여기 텍스트만 바꾸면 됨
        LegendDot(label = "평균", color = Color.Black.copy(alpha = 0.6f), dashed = true)
    }
}

@Composable
private fun LegendDot(label: String, color: Color, dashed: Boolean = false) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Canvas(Modifier.size(14.dp)) {
            if (dashed) {
                drawLine(
                    color = color,
                    start = Offset(0f, size.height / 2f),
                    end = Offset(size.width, size.height / 2f),
                    strokeWidth = 5f,
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 8f), 0f)
                )
            } else {
                drawCircle(color = color, radius = size.minDimension / 2f)
            }
        }
        Spacer(Modifier.width(6.dp))
        Text(label, fontSize = 12.sp, color = Color(0xFF444444))
    }
}
