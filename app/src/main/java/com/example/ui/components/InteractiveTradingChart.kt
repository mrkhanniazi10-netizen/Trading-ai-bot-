package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.engine.IndicatorCalculator
import kotlin.math.abs
import com.example.model.Candle
import com.example.model.StrategyParameters
import com.example.ui.theme.IndicatorMacd
import com.example.ui.theme.IndicatorMaFast
import com.example.ui.theme.IndicatorMaSlow
import com.example.ui.theme.IndicatorRsi
import com.example.ui.theme.IndicatorSignal
import com.example.ui.theme.SignalBuy
import com.example.ui.theme.SignalSell
import com.example.ui.theme.TerminalBackground
import com.example.ui.theme.TerminalCardBorder
import com.example.ui.theme.TerminalSurface
import com.example.ui.theme.TerminalSurfaceVariant
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import java.util.Locale

@Composable
fun InteractiveTradingChart(
    candles: List<Candle>,
    params: StrategyParameters,
    modifier: Modifier = Modifier
) {
    if (candles.size < 5) return

    val prices = remember(candles) { candles.map { it.close } }
    val series = remember(candles, params) {
        IndicatorCalculator.computeAll(
            prices = prices,
            rsiPeriod = params.rsiPeriod,
            macdFast = params.macdFastPeriod,
            macdSlow = params.macdSlowPeriod,
            macdSignal = params.macdSignalPeriod,
            maFast = params.maFastPeriod,
            maSlow = params.maSlowPeriod,
            maType = params.maType
        )
    }

    // Interactive scrub state
    var selectedIndex by remember { mutableStateOf<Int?>(null) }
    val activeIndex = selectedIndex ?: (candles.size - 1)
    val activeCandle = candles.getOrNull(activeIndex) ?: candles.last()
    val activeRsi = series.rsi.getOrNull(activeIndex)
    val activeMacd = series.macd.getOrNull(activeIndex)
    val activeFastMA = series.fastMA.getOrNull(activeIndex)
    val activeSlowMA = series.slowMA.getOrNull(activeIndex)

    Column(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(TerminalSurface)
            .border(1.dp, TerminalCardBorder, RoundedCornerShape(14.dp))
            .padding(12.dp)
    ) {
        // Chart Header & Live Crosshair readout
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Price Action & MA Crossover",
                    color = TextPrimary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = String.format(Locale.US, "O: %.2f  H: %.2f  L: %.2f  C: %.2f",
                        activeCandle.open, activeCandle.high, activeCandle.low, activeCandle.close),
                    color = TextMuted,
                    fontSize = 10.5.sp,
                    fontWeight = FontWeight.Medium
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                ChartLegendItem(color = IndicatorMaFast, label = "MA ${params.maFastPeriod}")
                Spacer(modifier = Modifier.width(8.dp))
                ChartLegendItem(color = IndicatorMaSlow, label = "MA ${params.maSlowPeriod}")
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Primary Price Candlestick & MA Chart
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(180.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(TerminalBackground)
                .pointerInput(candles.size) {
                    detectTapGestures(
                        onTap = { offset ->
                            val idx = ((offset.x / size.width) * (candles.size - 1))
                                .toInt()
                                .coerceIn(0, candles.size - 1)
                            selectedIndex = idx
                        }
                    )
                }
                .pointerInput(candles.size) {
                    detectDragGestures(
                        onDrag = { change, _ ->
                            val idx = ((change.position.x / size.width) * (candles.size - 1))
                                .toInt()
                                .coerceIn(0, candles.size - 1)
                            selectedIndex = idx
                        },
                        onDragEnd = { selectedIndex = null }
                    )
                }
        ) {
            Canvas(modifier = Modifier.matchParentSize()) {
                val width = size.width
                val height = size.height
                val count = candles.size

                val minPrice = candles.minOf { it.low }
                val maxPrice = candles.maxOf { it.high }
                val priceRange = (maxPrice - minPrice).coerceAtLeast(0.001)

                val candleSlotWidth = width / count
                val candleBodyWidth = (candleSlotWidth * 0.65f).coerceAtLeast(2f)

                fun priceToY(price: Double): Float {
                    val norm = (price - minPrice) / priceRange
                    return (height - (norm.toFloat() * height * 0.88f) - (height * 0.06f))
                }

                // Grid lines
                val gridSteps = 4
                for (i in 0..gridSteps) {
                    val gy = (height / gridSteps) * i
                    drawLine(
                        color = TerminalCardBorder.copy(alpha = 0.5f),
                        start = Offset(0f, gy),
                        end = Offset(width, gy),
                        strokeWidth = 0.8f
                    )
                }

                // Draw Candlesticks
                candles.forEachIndexed { i, candle ->
                    val cx = (i * candleSlotWidth) + (candleSlotWidth / 2f)
                    val highY = priceToY(candle.high)
                    val lowY = priceToY(candle.low)
                    val openY = priceToY(candle.open)
                    val closeY = priceToY(candle.close)

                    val isBullish = candle.close >= candle.open
                    val candleColor = if (isBullish) SignalBuy else SignalSell

                    // Wick
                    drawLine(
                        color = candleColor,
                        start = Offset(cx, highY),
                        end = Offset(cx, lowY),
                        strokeWidth = 1.2f
                    )

                    // Body
                    val topY = minOf(openY, closeY)
                    val bodyHeight = maxOf(abs(closeY - openY), 2f)
                    drawRect(
                        color = candleColor,
                        topLeft = Offset(cx - (candleBodyWidth / 2f), topY),
                        size = Size(candleBodyWidth, bodyHeight)
                    )
                }

                // Fast MA Line
                drawIndicatorLine(
                    series.fastMA,
                    count,
                    width,
                    ::priceToY,
                    IndicatorMaFast,
                    strokeWidth = 2.dp.toPx()
                )

                // Slow MA Line
                drawIndicatorLine(
                    series.slowMA,
                    count,
                    width,
                    ::priceToY,
                    IndicatorMaSlow,
                    strokeWidth = 2.dp.toPx()
                )

                // Crosshair cursor if scrubbing
                if (selectedIndex != null) {
                    val crossX = (activeIndex * candleSlotWidth) + (candleSlotWidth / 2f)
                    val crossY = priceToY(activeCandle.close)
                    drawLine(
                        color = Color.White.copy(alpha = 0.5f),
                        start = Offset(crossX, 0f),
                        end = Offset(crossX, height),
                        strokeWidth = 1f,
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f))
                    )
                    drawCircle(
                        color = Color.White,
                        radius = 4.dp.toPx(),
                        center = Offset(crossX, crossY)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // 2. RSI Sub-Chart
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(IndicatorRsi)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "RSI (${params.rsiPeriod})",
                    color = TextPrimary,
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            Text(
                text = activeRsi?.let { String.format(Locale.US, "%.1f", it) } ?: "--",
                color = when {
                    activeRsi == null -> TextMuted
                    activeRsi <= params.rsiOversold -> SignalBuy
                    activeRsi >= params.rsiOverbought -> SignalSell
                    else -> IndicatorRsi
                },
                fontSize = 12.sp,
                fontWeight = FontWeight.ExtraBold
            )
        }

        Spacer(modifier = Modifier.height(4.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(65.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(TerminalBackground)
        ) {
            Canvas(modifier = Modifier.matchParentSize()) {
                val width = size.width
                val height = size.height

                fun rsiToY(v: Double): Float {
                    return height - (v.toFloat() / 100f * height)
                }

                val overboughtY = rsiToY(params.rsiOverbought)
                val oversoldY = rsiToY(params.rsiOversold)
                val midY = rsiToY(50.0)

                // Shaded threshold band between oversold and overbought
                drawRect(
                    color = IndicatorRsi.copy(alpha = 0.06f),
                    topLeft = Offset(0f, overboughtY),
                    size = Size(width, oversoldY - overboughtY)
                )

                // Overbought line (70)
                drawLine(
                    color = SignalSell.copy(alpha = 0.6f),
                    start = Offset(0f, overboughtY),
                    end = Offset(width, overboughtY),
                    strokeWidth = 1f,
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(4f, 4f))
                )

                // Oversold line (30)
                drawLine(
                    color = SignalBuy.copy(alpha = 0.6f),
                    start = Offset(0f, oversoldY),
                    end = Offset(width, oversoldY),
                    strokeWidth = 1f,
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(4f, 4f))
                )

                // 50 Midline
                drawLine(
                    color = TerminalCardBorder.copy(alpha = 0.5f),
                    start = Offset(0f, midY),
                    end = Offset(width, midY),
                    strokeWidth = 0.8f
                )

                // RSI curve
                val count = series.rsi.size
                val stepX = width / (count - 1)
                val path = Path()
                var started = false

                series.rsi.forEachIndexed { i, value ->
                    if (value != null) {
                        val x = i * stepX
                        val y = rsiToY(value)
                        if (!started) {
                            path.moveTo(x, y)
                            started = true
                        } else {
                            path.lineTo(x, y)
                        }
                    }
                }

                drawPath(
                    path = path,
                    color = IndicatorRsi,
                    style = Stroke(width = 1.8.dp.toPx(), cap = StrokeCap.Round)
                )

                // Crosshair point
                if (selectedIndex != null && activeRsi != null) {
                    val cx = activeIndex * stepX
                    val cy = rsiToY(activeRsi)
                    drawCircle(color = IndicatorRsi, radius = 3.5.dp.toPx(), center = Offset(cx, cy))
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // 3. MACD Sub-Chart
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(IndicatorMacd)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "MACD (${params.macdFastPeriod},${params.macdSlowPeriod},${params.macdSignalPeriod})",
                    color = TextPrimary,
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.width(8.dp))
                ChartLegendItem(color = IndicatorSignal, label = "Sig")
            }

            Text(
                text = activeMacd?.let {
                    String.format(Locale.US, "Hist: %+.2f", it.histogram)
                } ?: "--",
                color = when {
                    activeMacd == null -> TextMuted
                    activeMacd.histogram >= 0 -> SignalBuy
                    else -> SignalSell
                },
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.height(4.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(65.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(TerminalBackground)
        ) {
            Canvas(modifier = Modifier.matchParentSize()) {
                val width = size.width
                val height = size.height
                val count = series.macd.size

                val validMacd = series.macd.filterNotNull()
                if (validMacd.isEmpty()) return@Canvas

                val maxHist = validMacd.maxOf { abs(it.histogram) }.coerceAtLeast(0.01)
                val maxLine = validMacd.maxOf { maxOf(abs(it.macd), abs(it.signal)) }.coerceAtLeast(0.01)
                val scale = maxOf(maxHist, maxLine) * 1.25

                fun valToY(v: Double): Float {
                    val norm = (v / scale).toFloat()
                    return (height / 2f) - (norm * (height / 2f))
                }

                val zeroY = height / 2f
                drawLine(
                    color = TerminalCardBorder,
                    start = Offset(0f, zeroY),
                    end = Offset(width, zeroY),
                    strokeWidth = 1f
                )

                val slotWidth = width / count
                val barWidth = (slotWidth * 0.6f).coerceAtLeast(1.5f)

                // Draw Histograms
                series.macd.forEachIndexed { i, pt ->
                    if (pt != null) {
                        val cx = (i * slotWidth) + (slotWidth / 2f)
                        val barY = valToY(pt.histogram)
                        val top = minOf(zeroY, barY)
                        val bHeight = maxOf(abs(barY - zeroY), 1.5f)
                        val barColor = if (pt.histogram >= 0) SignalBuy else SignalSell

                        drawRect(
                            color = barColor.copy(alpha = 0.8f),
                            topLeft = Offset(cx - (barWidth / 2f), top),
                            size = Size(barWidth, bHeight)
                        )
                    }
                }

                // Draw MACD Line (Cyan)
                val macdPath = Path()
                var startedMacd = false
                series.macd.forEachIndexed { i, pt ->
                    if (pt != null) {
                        val cx = (i * slotWidth) + (slotWidth / 2f)
                        val cy = valToY(pt.macd)
                        if (!startedMacd) {
                            macdPath.moveTo(cx, cy)
                            startedMacd = true
                        } else {
                            macdPath.lineTo(cx, cy)
                        }
                    }
                }
                drawPath(
                    path = macdPath,
                    color = IndicatorMacd,
                    style = Stroke(width = 1.6.dp.toPx(), cap = StrokeCap.Round)
                )

                // Draw Signal Line (Amber)
                val sigPath = Path()
                var startedSig = false
                series.macd.forEachIndexed { i, pt ->
                    if (pt != null) {
                        val cx = (i * slotWidth) + (slotWidth / 2f)
                        val cy = valToY(pt.signal)
                        if (!startedSig) {
                            sigPath.moveTo(cx, cy)
                            startedSig = true
                        } else {
                            sigPath.lineTo(cx, cy)
                        }
                    }
                }
                drawPath(
                    path = sigPath,
                    color = IndicatorSignal,
                    style = Stroke(width = 1.6.dp.toPx(), cap = StrokeCap.Round)
                )
            }
        }
    }
}

@Composable
private fun ChartLegendItem(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(6.dp)
                .clip(CircleShape)
                .background(color)
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = label,
            color = TextSecondary,
            fontSize = 10.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawIndicatorLine(
    values: List<Double?>,
    totalCount: Int,
    totalWidth: Float,
    priceToY: (Double) -> Float,
    color: Color,
    strokeWidth: Float
) {
    val stepX = totalWidth / totalCount
    val path = Path()
    var started = false

    values.forEachIndexed { i, v ->
        if (v != null) {
            val cx = (i * stepX) + (stepX / 2f)
            val cy = priceToY(v)
            if (!started) {
                path.moveTo(cx, cy)
                started = true
            } else {
                path.lineTo(cx, cy)
            }
        }
    }

    drawPath(
        path = path,
        color = color,
        style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
    )
}
