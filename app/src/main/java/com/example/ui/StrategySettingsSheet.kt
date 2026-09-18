package com.example.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoGraph
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.MAType
import com.example.model.StrategyMode
import com.example.model.StrategyParameters
import com.example.model.StrategyPreset
import com.example.ui.theme.IndicatorMacd
import com.example.ui.theme.IndicatorMaFast
import com.example.ui.theme.IndicatorRsi
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
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StrategySettingsSheet(
    currentParams: StrategyParameters,
    onSave: (StrategyParameters) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    // Local mutable copies for real-time adjusting
    var rsiPeriod by remember(currentParams) { mutableIntStateOf(currentParams.rsiPeriod) }
    var rsiOversold by remember(currentParams) { mutableDoubleStateOf(currentParams.rsiOversold) }
    var rsiOverbought by remember(currentParams) { mutableDoubleStateOf(currentParams.rsiOverbought) }

    var macdFast by remember(currentParams) { mutableIntStateOf(currentParams.macdFastPeriod) }
    var macdSlow by remember(currentParams) { mutableIntStateOf(currentParams.macdSlowPeriod) }
    var macdSignal by remember(currentParams) { mutableIntStateOf(currentParams.macdSignalPeriod) }

    var maFast by remember(currentParams) { mutableIntStateOf(currentParams.maFastPeriod) }
    var maSlow by remember(currentParams) { mutableIntStateOf(currentParams.maSlowPeriod) }
    var maType by remember(currentParams) { mutableStateOf(currentParams.maType) }

    var strategyMode by remember(currentParams) { mutableStateOf(currentParams.strategyMode) }
    var minConfidence by remember(currentParams) { mutableIntStateOf(currentParams.minConfidenceThreshold) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = TerminalSurface,
        dragHandle = null
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
                .verticalScroll(rememberScrollState())
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Tune,
                        contentDescription = "Strategy Parameters",
                        tint = SignalBuy,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "Strategy Parameters",
                            color = TextPrimary,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Customize lookback periods & signal thresholds",
                            color = TextSecondary,
                            fontSize = 12.sp
                        )
                    }
                }

                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Close", tint = TextSecondary)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Presets row
            Text(
                text = "STRATEGY PRESETS",
                color = TextMuted,
                fontSize = 11.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 1.sp
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                StrategyPreset.entries.forEach { preset ->
                    PresetCard(
                        preset = preset,
                        isSelected = currentParams == preset.params,
                        modifier = Modifier.weight(1f),
                        onClick = {
                            rsiPeriod = preset.params.rsiPeriod
                            rsiOversold = preset.params.rsiOversold
                            rsiOverbought = preset.params.rsiOverbought
                            macdFast = preset.params.macdFastPeriod
                            macdSlow = preset.params.macdSlowPeriod
                            macdSignal = preset.params.macdSignalPeriod
                            maFast = preset.params.maFastPeriod
                            maSlow = preset.params.maSlowPeriod
                            maType = preset.params.maType
                            strategyMode = preset.params.strategyMode
                            minConfidence = preset.params.minConfidenceThreshold
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))
            HorizontalDivider(color = TerminalCardBorder)
            Spacer(modifier = Modifier.height(16.dp))

            // 1. RSI Indicator Section
            IndicatorSectionHeader(
                title = "Relative Strength Index (RSI)",
                accentColor = IndicatorRsi
            )
            ParameterSlider(
                label = "Lookback Period",
                valueDisplay = "$rsiPeriod bars",
                value = rsiPeriod.toFloat(),
                range = 2f..50f,
                steps = 47,
                accentColor = IndicatorRsi,
                onValueChange = { rsiPeriod = it.roundToInt() }
            )
            ParameterSlider(
                label = "Oversold Threshold (BUY trigger)",
                valueDisplay = String.format(Locale.US, "%.0f", rsiOversold),
                value = rsiOversold.toFloat(),
                range = 15f..45f,
                steps = 29,
                accentColor = SignalBuy,
                onValueChange = { rsiOversold = it.toDouble().coerceAtMost(rsiOverbought - 10.0) }
            )
            ParameterSlider(
                label = "Overbought Threshold (SELL trigger)",
                valueDisplay = String.format(Locale.US, "%.0f", rsiOverbought),
                value = rsiOverbought.toFloat(),
                range = 55f..85f,
                steps = 29,
                accentColor = SignalSell,
                onValueChange = { rsiOverbought = it.toDouble().coerceAtLeast(rsiOversold + 10.0) }
            )

            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider(color = TerminalCardBorder)
            Spacer(modifier = Modifier.height(16.dp))

            // 2. MACD Section
            IndicatorSectionHeader(
                title = "MACD (Convergence / Divergence)",
                accentColor = IndicatorMacd
            )
            ParameterSlider(
                label = "Fast EMA Period",
                valueDisplay = "$macdFast bars",
                value = macdFast.toFloat(),
                range = 2f..40f,
                steps = 37,
                accentColor = IndicatorMacd,
                onValueChange = { macdFast = it.roundToInt().coerceAtMost(macdSlow - 1) }
            )
            ParameterSlider(
                label = "Slow EMA Period",
                valueDisplay = "$macdSlow bars",
                value = macdSlow.toFloat(),
                range = 10f..80f,
                steps = 69,
                accentColor = IndicatorMacd,
                onValueChange = { macdSlow = it.roundToInt().coerceAtLeast(macdFast + 1) }
            )
            ParameterSlider(
                label = "Signal Smoothing Period",
                valueDisplay = "$macdSignal bars",
                value = macdSignal.toFloat(),
                range = 2f..25f,
                steps = 22,
                accentColor = IndicatorMacd,
                onValueChange = { macdSignal = it.roundToInt() }
            )

            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider(color = TerminalCardBorder)
            Spacer(modifier = Modifier.height(16.dp))

            // 3. Moving Average Crossover Section
            IndicatorSectionHeader(
                title = "Moving Average Crossover Logic",
                accentColor = IndicatorMaFast
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "MA Type:",
                    color = TextSecondary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
                MAType.entries.forEach { type ->
                    FilterChip(
                        selected = maType == type,
                        onClick = { maType = type },
                        label = { Text(type.name, fontSize = 11.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = IndicatorMaFast.copy(alpha = 0.2f),
                            selectedLabelColor = IndicatorMaFast
                        )
                    )
                }
            }

            ParameterSlider(
                label = "Fast MA Lookback",
                valueDisplay = "$maFast bars",
                value = maFast.toFloat(),
                range = 2f..50f,
                steps = 47,
                accentColor = IndicatorMaFast,
                onValueChange = { maFast = it.roundToInt().coerceAtMost(maSlow - 1) }
            )
            ParameterSlider(
                label = "Slow MA Lookback",
                valueDisplay = "$maSlow bars",
                value = maSlow.toFloat(),
                range = 10f..120f,
                steps = 109,
                accentColor = IndicatorMaFast,
                onValueChange = { maSlow = it.roundToInt().coerceAtLeast(maFast + 1) }
            )

            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider(color = TerminalCardBorder)
            Spacer(modifier = Modifier.height(16.dp))

            // 4. Strategy Mode & Confluence
            IndicatorSectionHeader(
                title = "Confluence & Signal Aggregation",
                accentColor = SignalBuy
            )
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                StrategyMode.entries.forEach { mode ->
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (strategyMode == mode) TerminalSurfaceVariant else Color.Transparent)
                            .border(
                                1.dp,
                                if (strategyMode == mode) SignalBuy else TerminalCardBorder,
                                RoundedCornerShape(8.dp)
                            )
                            .clickable { strategyMode = mode }
                            .padding(10.dp)
                    ) {
                        Column {
                            Text(
                                text = mode.title,
                                color = if (strategyMode == mode) SignalBuy else TextPrimary,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = mode.description,
                                color = TextMuted,
                                fontSize = 11.sp
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            ParameterSlider(
                label = "Minimum Confidence Threshold",
                valueDisplay = "$minConfidence%",
                value = minConfidence.toFloat(),
                range = 50f..90f,
                steps = 39,
                accentColor = SignalBuy,
                onValueChange = { minConfidence = it.roundToInt() }
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Action buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = {
                        val default = StrategyPreset.BALANCED_SWING.params
                        rsiPeriod = default.rsiPeriod
                        rsiOversold = default.rsiOversold
                        rsiOverbought = default.rsiOverbought
                        macdFast = default.macdFastPeriod
                        macdSlow = default.macdSlowPeriod
                        macdSignal = default.macdSignalPeriod
                        maFast = default.maFastPeriod
                        maSlow = default.maSlowPeriod
                        maType = default.maType
                        strategyMode = default.strategyMode
                        minConfidence = default.minConfidenceThreshold
                    },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = TextSecondary)
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = "Reset", modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Reset")
                }

                Button(
                    onClick = {
                        val updated = StrategyParameters(
                            rsiPeriod = rsiPeriod,
                            rsiOversold = rsiOversold,
                            rsiOverbought = rsiOverbought,
                            macdFastPeriod = macdFast,
                            macdSlowPeriod = macdSlow,
                            macdSignalPeriod = macdSignal,
                            maFastPeriod = maFast,
                            maSlowPeriod = maSlow,
                            maType = maType,
                            strategyMode = strategyMode,
                            minConfidenceThreshold = minConfidence
                        )
                        onSave(updated)
                        onDismiss()
                    },
                    modifier = Modifier
                        .weight(2f)
                        .testTag("apply_strategy_button"),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = SignalBuy,
                        contentColor = Color.Black
                    )
                ) {
                    Icon(Icons.Default.AutoGraph, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Apply Parameters", fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun PresetCard(
    preset: StrategyPreset,
    isSelected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(if (isSelected) SignalBuy.copy(alpha = 0.15f) else TerminalSurfaceVariant)
            .border(
                1.dp,
                if (isSelected) SignalBuy else TerminalCardBorder,
                RoundedCornerShape(8.dp)
            )
            .clickable(onClick = onClick)
            .padding(8.dp)
    ) {
        Column {
            Text(
                text = preset.title,
                color = if (isSelected) SignalBuy else TextPrimary,
                fontSize = 11.5.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = when (preset) {
                    StrategyPreset.BALANCED_SWING -> "14 RSI · 12/26 MACD"
                    StrategyPreset.AGGRESSIVE_SCALP -> "7 RSI · 6/13 MACD"
                    StrategyPreset.CONSERVATIVE_TREND -> "21 RSI · 19/39 MACD"
                },
                color = TextMuted,
                fontSize = 9.5.sp,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun IndicatorSectionHeader(
    title: String,
    accentColor: Color
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(7.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(accentColor)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = title,
            color = TextPrimary,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun ParameterSlider(
    label: String,
    valueDisplay: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    steps: Int,
    accentColor: Color,
    onValueChange: (Float) -> Unit
) {
    Column(modifier = Modifier.padding(vertical = 4.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                color = TextSecondary,
                fontSize = 11.5.sp
            )
            Text(
                text = valueDisplay,
                color = accentColor,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )
        }
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = range,
            steps = steps,
            colors = SliderDefaults.colors(
                thumbColor = accentColor,
                activeTrackColor = accentColor,
                inactiveTrackColor = TerminalCardBorder
            ),
            modifier = Modifier.height(26.dp)
        )
    }
}
