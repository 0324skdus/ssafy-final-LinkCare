package com.a307.linkcare.common.component.molecules

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
import com.a307.linkcare.feature.caregroup.domain.MemberSeries
import com.a307.linkcare.feature.caregroup.domain.WeeklyChartData

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

                    // 값 범위
                    val allValues = displayedSeries.flatMap { it.points } + data.avgSeries.points
                    val minV = (allValues.minOrNull() ?: 0f)
                    val maxV = (allValues.maxOrNull() ?: 1f)
                    val range = (maxV - minV).takeIf { it > 0f } ?: 1f

                    // Y축 4분할 눈금 (숫자만 표기)
                    val steps = 4
                    repeat(steps + 1) { i ->
                        val y = paddingTop + h - (h / steps) * i
                        drawLine(
                            color = Color(0x11000000),
                            start = Offset(paddingLeft, y),
                            end = Offset(size.width - paddingRight, y),
                            strokeWidth = 1f
                        )
                        val v = minV + range / steps * i
                        val space = 2.dp.toPx()
                        drawContext.canvas.nativeCanvas.apply {
                            drawText(
                                v.toInt().toString(),
                                paddingLeft - 4f,
                                y + 4f,
                                android.graphics.Paint().apply {
                                    color = 0xFF666666.toInt()
                                    textSize = 28f
                                    textAlign = android.graphics.Paint.Align.RIGHT
                                }
                            )
                        }
                    }

                    // X축 (data.xLabels가 월~일 순서라고 가정)
                    val xStep = w / (data.xLabels.size - 1).coerceAtLeast(1)
                    data.xLabels.forEachIndexed { i, label ->
                        val x = paddingLeft + i * xStep
                        val y = size.height - paddingBottom / 2f
                        val space = 2.dp.toPx()
                        drawContext.canvas.nativeCanvas.apply {
                            drawText(
                                label,
                                x,
                                y + 20f,
                                android.graphics.Paint().apply {
                                    color = 0xFF666666.toInt()
                                    textSize = 28f
                                    textAlign = android.graphics.Paint.Align.CENTER
                                }
                            )
                        }
                    }

                    fun pt(i: Int, v: Float): Offset {
                        val x = paddingLeft + i * xStep
                        val y = paddingTop + h - ((v - minV) / range) * h
                        return Offset(x, y)
                    }

                    // 시리즈 라인
                    displayedSeries.forEachIndexed { idx, s ->
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

                    // 평균 (점선)
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
