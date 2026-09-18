package com.example.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoGraph
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
import com.example.model.IndicatorDiagnosis
import com.example.model.PaperTrade
import com.example.model.SignalType
import com.example.model.StrategyParameters
import com.example.model.Ticker
import com.example.model.TradingSignal
import com.example.ui.components.ConfidenceMeter
import com.example.ui.components.InteractiveTradingChart
import com.example.ui.components.SignalBadge
import com.example.ui.theme.IndicatorMacd
import com.example.ui.theme.IndicatorMaFast
import com.example.ui.theme.IndicatorRsi
import com.example.ui.theme.SignalBuy
import com.example.ui.theme.SignalHold
import com.example.ui.theme.SignalSell
import com.example.ui.theme.TerminalBackground
import com.example.ui.theme.TerminalCardBorder
import com.example.ui.theme.TerminalSurface
import com.example.ui.theme.TerminalSurfaceVariant
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TickerDetailScreen(
    ticker: Ticker,
    signal: TradingSignal?,
    strategyParams: StrategyParameters,
    paperTrades: List<PaperTrade>,
    onBack: () -> Unit,
    onOpenTuneSettings: () -> Unit,
    onExecuteTrade: (Ticker, TradingSignal) -> Unit,
    onCloseTrade: (String) -> Unit
) {
    var showExecutedSnackbar by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column {
                            Text(
                                text = ticker.symbol,
                                color = TextPrimary,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.ExtraBold
                            )
                            Text(
                                text = ticker.name,
                                color = TextSecondary,
                                fontSize = 12.sp
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = TextPrimary
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = onOpenTuneSettings,
                        modifier = Modifier.testTag("tune_params_detail_button")
                    ) {
                        Icon(Icons.Default.Tune, contentDescription = "Tune Strategy", tint = SignalBuy)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = TerminalSurface)
            )
        },
        containerColor = TerminalBackground
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Spacer(modifier = Modifier.height(4.dp))
                // Price & 24h Summary Bar
                TickerPriceSummary(ticker)
            }

            // Hero Signal Card
            if (signal != null) {
                item {
                    SignalHeroCard(
                        signal = signal,
                        onTuneClick = onOpenTuneSettings,
                        onTradeClick = {
                            onExecuteTrade(ticker, signal)
                            showExecutedSnackbar = true
                        }
                    )
                }

                // Execution Confirmation
                if (showExecutedSnackbar) {
                    item {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = SignalBuy.copy(alpha = 0.15f)),
                            border = androidx.compose.foundation.BorderStroke(1.dp, SignalBuy),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = SignalBuy)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Simulated ${signal.signalType.name} executed at \$${String.format(Locale.US, "%.2f", ticker.currentPrice)}",
                                    color = SignalBuy,
                                    fontSize = 12.5.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }

                // Indicator Diagnostic Breakdown Cards (RSI, MACD, MA Crossover)
                item {
                    Text(
                        text = "INDICATOR STRATEGY DIAGNOSTICS",
                        color = TextMuted,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 1.sp
                    )
                }

                item {
                    IndicatorDiagnosticsSection(
                        diagnosis = signal.diagnosis,
                        params = strategyParams
                    )
                }
            }

            // Interactive Technical Chart with Candlesticks, MA, RSI, MACD
            item {
                Text(
                    text = "TECHNICAL CHARTS & OSCILLATORS",
                    color = TextMuted,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 1.sp
                )
            }

            item {
                InteractiveTradingChart(
                    candles = ticker.history,
                    params = strategyParams
                )
            }

            // Active Simulated Paper Trades for this ticker
            val tickerTrades = paperTrades.filter { it.symbol == ticker.symbol }
            if (tickerTrades.isNotEmpty()) {
                item {
                    Text(
                        text = "ACTIVE SIMULATED POSITIONS (${tickerTrades.size})",
                        color = TextMuted,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 1.sp
                    )
                }

                items(tickerTrades.size) { idx ->
                    val trade = tickerTrades[idx]
                    PaperTradeCard(trade = trade, onClose = { onCloseTrade(trade.id) })
                }
            }

            item {
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

@Composable
private fun TickerPriceSummary(ticker: Ticker) {
    val isPositive = ticker.isPositive
    val changeColor = if (isPositive) SignalBuy else SignalSell
    val sign = if (isPositive) "+" else ""

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = TerminalSurface),
        border = androidx.compose.foundation.BorderStroke(1.dp, TerminalCardBorder),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Market Price",
                    color = TextSecondary,
                    fontSize = 11.sp
                )
                Text(
                    text = String.format(Locale.US, "$%,.2f", ticker.currentPrice),
                    color = TextPrimary,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.ExtraBold
                )
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "$sign${String.format(Locale.US, "%.2f", ticker.change24hPercent)}%",
                    color = changeColor,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "24h H: $${String.format(Locale.US, "%.1f", ticker.high24h)} · L: $${String.format(Locale.US, "%.1f", ticker.low24h)}",
                    color = TextMuted,
                    fontSize = 10.5.sp
                )
            }
        }
    }
}

@Composable
private fun SignalHeroCard(
    signal: TradingSignal,
    onTuneClick: () -> Unit,
    onTradeClick: () -> Unit
) {
    val borderColor = when (signal.signalType) {
        SignalType.BUY -> SignalBuy
        SignalType.SELL -> SignalSell
        SignalType.HOLD -> SignalHold
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = TerminalSurface),
        border = androidx.compose.foundation.BorderStroke(1.5.dp, borderColor.copy(alpha = 0.8f)),
        shape = RoundedCornerShape(14.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Signal Type & Strength Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "AI BOT SIGNAL GENERATION",
                        color = TextMuted,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "${signal.strength.label} Confluence Signal",
                        color = TextSecondary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                }

                SignalBadge(
                    signalType = signal.signalType,
                    confidence = signal.confidence,
                    isLarge = true
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Strategy Rationale Summary
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(TerminalSurfaceVariant)
                    .border(0.8.dp, TerminalCardBorder, RoundedCornerShape(8.dp))
                    .padding(12.dp)
            ) {
                Text(
                    text = signal.summary,
                    color = TextPrimary,
                    fontSize = 12.5.sp,
                    lineHeight = 18.sp
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Confidence Progress Bar
            ConfidenceMeter(
                confidence = signal.confidence,
                signalType = signal.signalType
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = onTuneClick,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = TextSecondary)
                ) {
                    Icon(Icons.Default.Tune, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Adjust Params", fontSize = 12.sp)
                }

                if (signal.signalType != SignalType.HOLD) {
                    Button(
                        onClick = onTradeClick,
                        modifier = Modifier.weight(1.3f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (signal.signalType == SignalType.BUY) SignalBuy else SignalSell,
                            contentColor = if (signal.signalType == SignalType.BUY) Color.Black else Color.White
                        )
                    ) {
                        Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Simulate ${signal.signalType.name}",
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.5.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun IndicatorDiagnosticsSection(
    diagnosis: IndicatorDiagnosis,
    params: StrategyParameters
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        // 1. RSI Card
        DiagnosticCard(
            title = "Relative Strength Index (RSI ${params.rsiPeriod})",
            signal = diagnosis.rsiSignal,
            value = String.format(Locale.US, "%.1f", diagnosis.rsiValue),
            reason = diagnosis.rsiReason,
            accentColor = IndicatorRsi
        ) {
            // Visual RSI Gauge Bar
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(text = "Oversold (<${params.rsiOversold.toInt()})", color = SignalBuy, fontSize = 10.sp)
                    Text(text = "Neutral (50)", color = TextMuted, fontSize = 10.sp)
                    Text(text = "Overbought (>${params.rsiOverbought.toInt()})", color = SignalSell, fontSize = 10.sp)
                }
                Spacer(modifier = Modifier.height(4.dp))
                LinearProgressIndicator(
                    progress = { (diagnosis.rsiValue / 100.0).toFloat().coerceIn(0f, 1f) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp)),
                    color = when {
                        diagnosis.isRsiOversold -> SignalBuy
                        diagnosis.isRsiOverbought -> SignalSell
                        else -> IndicatorRsi
                    },
                    trackColor = TerminalCardBorder
                )
            }
        }

        // 2. MACD Card
        DiagnosticCard(
            title = "MACD (${params.macdFastPeriod}, ${params.macdSlowPeriod}, ${params.macdSignalPeriod})",
            signal = diagnosis.macdSignal,
            value = String.format(Locale.US, "Hist: %+.2f", diagnosis.macdHistogram),
            reason = diagnosis.macdReason,
            accentColor = IndicatorMacd
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "MACD Line: ${String.format(Locale.US, "%.2f", diagnosis.macdLine)}",
                    color = IndicatorMacd,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = "Signal Line: ${String.format(Locale.US, "%.2f", diagnosis.macdSignalLine)}",
                    color = TextSecondary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = "Histogram: ${String.format(Locale.US, "%+.2f", diagnosis.macdHistogram)}",
                    color = if (diagnosis.macdHistogram >= 0) SignalBuy else SignalSell,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        // 3. Moving Average Crossover Card
        DiagnosticCard(
            title = "MA Crossover (${params.maFastPeriod} / ${params.maSlowPeriod} ${params.maType.name})",
            signal = diagnosis.maSignal,
            value = if (diagnosis.fastMA > diagnosis.slowMA) "Bullish Trend" else "Bearish Trend",
            reason = diagnosis.maReason,
            accentColor = IndicatorMaFast
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Fast MA (${params.maFastPeriod}): $${String.format(Locale.US, "%.2f", diagnosis.fastMA)}",
                    color = IndicatorMaFast,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = "Slow MA (${params.maSlowPeriod}): $${String.format(Locale.US, "%.2f", diagnosis.slowMA)}",
                    color = IndicatorMaFast.copy(alpha = 0.7f),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

@Composable
private fun DiagnosticCard(
    title: String,
    signal: SignalType,
    value: String,
    reason: String,
    accentColor: Color,
    content: @Composable () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = TerminalSurface),
        border = androidx.compose.foundation.BorderStroke(1.dp, TerminalCardBorder),
        shape = RoundedCornerShape(10.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(7.dp)
                            .clip(CircleShape)
                            .background(accentColor)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = title,
                        color = TextPrimary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                SignalBadge(signalType = signal)
            }

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = reason,
                color = TextSecondary,
                fontSize = 11.5.sp,
                fontWeight = FontWeight.Medium
            )

            Spacer(modifier = Modifier.height(8.dp))
            content()
        }
    }
}

@Composable
private fun PaperTradeCard(
    trade: PaperTrade,
    onClose: () -> Unit
) {
    val isProfit = trade.pnlAmount >= 0
    val pnlColor = if (isProfit) SignalBuy else SignalSell
    val sign = if (isProfit) "+" else ""

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = TerminalSurfaceVariant),
        border = androidx.compose.foundation.BorderStroke(1.dp, TerminalCardBorder),
        shape = RoundedCornerShape(10.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    SignalBadge(signalType = trade.type)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Entry: $${String.format(Locale.US, "%.2f", trade.entryPrice)}",
                        color = TextSecondary,
                        fontSize = 12.sp
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "P&L: $sign$${String.format(Locale.US, "%.2f", trade.pnlAmount)} ($sign${String.format(Locale.US, "%.2f", trade.pnlPercent)}%)",
                    color = pnlColor,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Button(
                onClick = onClose,
                colors = ButtonDefaults.buttonColors(containerColor = TerminalCardBorder, contentColor = TextPrimary),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("Close", fontSize = 11.sp)
            }
        }
    }
}
