package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.HorizontalRule
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.SignalStrength
import com.example.model.SignalType
import com.example.ui.theme.SignalBuy
import com.example.ui.theme.SignalBuyGlow
import com.example.ui.theme.SignalHold
import com.example.ui.theme.SignalHoldGlow
import com.example.ui.theme.SignalSell
import com.example.ui.theme.SignalSellGlow
import com.example.ui.theme.TerminalCardBorder
import com.example.ui.theme.TerminalSurfaceVariant
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun SignalBadge(
    signalType: SignalType,
    strength: SignalStrength? = null,
    confidence: Int? = null,
    modifier: Modifier = Modifier,
    isLarge: Boolean = false
) {
    val (bgColor, textColor, glowColor, icon) = when (signalType) {
        SignalType.BUY -> Quad(
            SignalBuy.copy(alpha = 0.18f),
            SignalBuy,
            SignalBuyGlow,
            Icons.Default.ArrowUpward
        )
        SignalType.SELL -> Quad(
            SignalSell.copy(alpha = 0.18f),
            SignalSell,
            SignalSellGlow,
            Icons.Default.ArrowDownward
        )
        SignalType.HOLD -> Quad(
            SignalHold.copy(alpha = 0.18f),
            SignalHold,
            SignalHoldGlow,
            Icons.Default.HorizontalRule
        )
    }

    Surface(
        modifier = modifier
            .testTag("signal_badge_${signalType.name}")
            .clip(RoundedCornerShape(if (isLarge) 12.dp else 8.dp))
            .border(
                width = if (isLarge) 1.5.dp else 1.dp,
                color = textColor.copy(alpha = 0.6f),
                shape = RoundedCornerShape(if (isLarge) 12.dp else 8.dp)
            ),
        color = bgColor
    ) {
        Row(
            modifier = Modifier.padding(
                horizontal = if (isLarge) 14.dp else 8.dp,
                vertical = if (isLarge) 8.dp else 4.dp
            ),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = signalType.label,
                tint = textColor,
                modifier = Modifier.size(if (isLarge) 20.dp else 14.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = signalType.label,
                color = textColor,
                fontWeight = FontWeight.ExtraBold,
                fontSize = if (isLarge) 16.sp else 12.sp,
                letterSpacing = 0.5.sp
            )
            if (confidence != null) {
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "$confidence%",
                    color = textColor.copy(alpha = 0.85f),
                    fontWeight = FontWeight.Medium,
                    fontSize = if (isLarge) 14.sp else 11.sp
                )
            }
        }
    }
}

private data class Quad<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)

@Composable
fun IndicatorStatusChip(
    name: String,
    value: String,
    signal: SignalType,
    modifier: Modifier = Modifier
) {
    val chipColor = when (signal) {
        SignalType.BUY -> SignalBuy
        SignalType.SELL -> SignalSell
        SignalType.HOLD -> TextSecondary
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(6.dp))
            .background(TerminalSurfaceVariant)
            .border(0.8.dp, TerminalCardBorder, RoundedCornerShape(6.dp))
            .padding(horizontal = 7.dp, vertical = 3.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(chipColor)
            )
            Spacer(modifier = Modifier.width(5.dp))
            Text(
                text = "$name: ",
                color = TextMuted,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = value,
                color = TextPrimary,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun SparklineChart(
    prices: List<Double>,
    isPositive: Boolean,
    modifier: Modifier = Modifier
) {
    if (prices.size < 2) return

    val strokeColor = if (isPositive) SignalBuy else SignalSell

    Canvas(modifier = modifier) {
        val minPrice = prices.minOrNull() ?: 0.0
        val maxPrice = prices.maxOrNull() ?: 1.0
        val range = (maxPrice - minPrice).coerceAtLeast(0.0001)

        val width = size.width
        val height = size.height
        val stepX = width / (prices.size - 1)

        val path = Path()
        val fillPath = Path()

        prices.forEachIndexed { index, price ->
            val x = index * stepX
            val normalizedY = (price - minPrice) / range
            val y = height - (normalizedY.toFloat() * height * 0.85f) - (height * 0.075f)

            if (index == 0) {
                path.moveTo(x, y)
                fillPath.moveTo(x, height)
                fillPath.lineTo(x, y)
            } else {
                path.lineTo(x, y)
                fillPath.lineTo(x, y)
            }
        }

        fillPath.lineTo(width, height)
        fillPath.close()

        // Gradient fill under the sparkline
        drawPath(
            path = fillPath,
            brush = Brush.verticalGradient(
                colors = listOf(strokeColor.copy(alpha = 0.25f), Color.Transparent),
                startY = 0f,
                endY = height
            )
        )

        // Line
        drawPath(
            path = path,
            color = strokeColor,
            style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round)
        )
    }
}

@Composable
fun ConfidenceMeter(
    confidence: Int,
    signalType: SignalType,
    modifier: Modifier = Modifier
) {
    val targetColor = when (signalType) {
        SignalType.BUY -> SignalBuy
        SignalType.SELL -> SignalSell
        SignalType.HOLD -> SignalHold
    }
    val animatedColor by animateColorAsState(targetValue = targetColor, label = "meter_color")

    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Strategy Confluence Confidence",
                color = TextSecondary,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = "$confidence%",
                color = animatedColor,
                fontSize = 13.sp,
                fontWeight = FontWeight.ExtraBold
            )
        }
        Spacer(modifier = Modifier.height(6.dp))
        LinearProgressIndicator(
            progress = { confidence / 100f },
            modifier = Modifier
                .fillMaxWidth()
                .height(7.dp)
                .clip(RoundedCornerShape(4.dp)),
            color = animatedColor,
            trackColor = TerminalCardBorder
        )
    }
}
