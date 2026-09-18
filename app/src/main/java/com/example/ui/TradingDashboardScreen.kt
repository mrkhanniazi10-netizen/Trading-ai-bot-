package com.example.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.FiberManualRecord
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
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
import com.example.model.MarketCategory
import com.example.model.SignalType
import com.example.model.StrategyParameters
import com.example.model.Ticker
import com.example.model.TradingSignal
import com.example.ui.components.IndicatorStatusChip
import com.example.ui.components.SignalAlertBanner
import com.example.ui.components.SignalBadge
import com.example.ui.components.SparklineChart
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
import com.example.viewmodel.DashboardUiState
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TradingDashboardScreen(
    state: DashboardUiState,
    onSelectTicker: (Ticker) -> Unit,
    onOpenTuneSettings: () -> Unit,
    onCategorySelected: (MarketCategory) -> Unit,
    onSignalFilterSelected: (SignalType?) -> Unit,
    onSearchQueryChanged: (String) -> Unit,
    onToggleLiveStreaming: () -> Unit,
    onDismissAlert: () -> Unit = {},
    onTriggerTestAlert: () -> Unit = {}
) {
    var isSearchExpanded by remember { mutableStateOf(false) }

    val filteredTickers = remember(
        state.tickers,
        state.signals,
        state.selectedCategory,
        state.selectedSignalFilter,
        state.searchQuery
    ) {
        state.tickers.filter { ticker ->
            val matchesCategory = state.selectedCategory == MarketCategory.ALL || ticker.category == state.selectedCategory
            val signal = state.signals[ticker.symbol]
            val matchesSignal = state.selectedSignalFilter == null || signal?.signalType == state.selectedSignalFilter
            val matchesSearch = state.searchQuery.isBlank() ||
                    ticker.symbol.contains(state.searchQuery, ignoreCase = true) ||
                    ticker.name.contains(state.searchQuery, ignoreCase = true)
            matchesCategory && matchesSignal && matchesSearch
        }
    }

    val buyCount = state.signals.values.count { it.signalType == SignalType.BUY }
    val sellCount = state.signals.values.count { it.signalType == SignalType.SELL }
    val holdCount = state.signals.values.count { it.signalType == SignalType.HOLD }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(SignalBuy.copy(alpha = 0.15f))
                                .border(1.dp, SignalBuy, RoundedCornerShape(8.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.SmartToy,
                                contentDescription = "AI Bot",
                                tint = SignalBuy,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Trading AI Bot",
                                color = TextPrimary,
                                fontSize = 17.sp,
                                fontWeight = FontWeight.ExtraBold
                            )
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .clip(CircleShape)
                                        .background(if (state.isLiveStreaming) SignalBuy else TextMuted)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = if (state.isLiveStreaming) "Live Signals Active" else "Simulation Paused",
                                    color = if (state.isLiveStreaming) SignalBuy else TextMuted,
                                    fontSize = 10.5.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                },
                actions = {
                    // Search toggle
                    IconButton(onClick = { isSearchExpanded = !isSearchExpanded }) {
                        Icon(Icons.Default.Search, contentDescription = "Search", tint = TextSecondary)
                    }

                    // Test signal alert trigger button
                    IconButton(
                        onClick = onTriggerTestAlert,
                        modifier = Modifier.testTag("test_signal_alert_button")
                    ) {
                        Icon(
                            Icons.Default.NotificationsActive,
                            contentDescription = "Trigger Signal Alert",
                            tint = SignalBuy
                        )
                    }

                    // Live stream toggle
                    IconButton(onClick = onToggleLiveStreaming) {
                        Icon(
                            imageVector = if (state.isLiveStreaming) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = "Toggle Stream",
                            tint = if (state.isLiveStreaming) SignalBuy else TextMuted
                        )
                    }

                    // Tune Strategy Parameters button
                    IconButton(
                        onClick = onOpenTuneSettings,
                        modifier = Modifier.testTag("tune_strategy_button")
                    ) {
                        Icon(
                            Icons.Default.Tune,
                            contentDescription = "Strategy Parameters",
                            tint = SignalBuy
                        )
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
                .padding(paddingValues),
            contentPadding = PaddingValues(bottom = 24.dp)
        ) {
            // Live AI Signal Alert Banner (if any generated)
            if (state.activeAlert != null) {
                item {
                    SignalAlertBanner(
                        alert = state.activeAlert,
                        onDismiss = onDismissAlert,
                        onViewTicker = { sym ->
                            val match = state.tickers.find { it.symbol == sym }
                            if (match != null) onSelectTicker(match)
                        }
                    )
                }
            }

            // Optional Search Bar
            if (isSearchExpanded) {
                item {
                    OutlinedTextField(
                        value = state.searchQuery,
                        onValueChange = onSearchQueryChanged,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        placeholder = { Text("Search by ticker or name...", color = TextMuted) },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = SignalBuy,
                            unfocusedBorderColor = TerminalCardBorder,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        ),
                        shape = RoundedCornerShape(10.dp)
                    )
                }
            }

            // Strategy Banner with Parameter Summary
            item {
                StrategyHeaderCard(
                    params = state.strategyParams,
                    buyCount = buyCount,
                    sellCount = sellCount,
                    holdCount = holdCount,
                    paperTradesCount = state.paperTrades.size,
                    totalPnl = state.totalProfitLoss,
                    onTuneClick = onOpenTuneSettings
                )
            }

            // Category Filter Chips
            item {
                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(MarketCategory.entries) { category ->
                        FilterChip(
                            selected = state.selectedCategory == category,
                            onClick = { onCategorySelected(category) },
                            label = { Text(category.label, fontSize = 11.5.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = SignalBuy.copy(alpha = 0.18f),
                                selectedLabelColor = SignalBuy,
                                containerColor = TerminalSurface,
                                labelColor = TextSecondary
                            ),
                            border = FilterChipDefaults.filterChipBorder(
                                enabled = true,
                                selected = state.selectedCategory == category,
                                borderColor = TerminalCardBorder,
                                selectedBorderColor = SignalBuy
                            )
                        )
                    }
                }
            }

            // Signal Filter Chips (All, BUY, SELL, HOLD)
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    SignalFilterPill(
                        label = "All Signals",
                        isSelected = state.selectedSignalFilter == null,
                        color = TextPrimary,
                        onClick = { onSignalFilterSelected(null) }
                    )
                    SignalFilterPill(
                        label = "BUY ($buyCount)",
                        isSelected = state.selectedSignalFilter == SignalType.BUY,
                        color = SignalBuy,
                        onClick = { onSignalFilterSelected(SignalType.BUY) }
                    )
                    SignalFilterPill(
                        label = "SELL ($sellCount)",
                        isSelected = state.selectedSignalFilter == SignalType.SELL,
                        color = SignalSell,
                        onClick = { onSignalFilterSelected(SignalType.SELL) }
                    )
                    SignalFilterPill(
                        label = "HOLD ($holdCount)",
                        isSelected = state.selectedSignalFilter == SignalType.HOLD,
                        color = SignalHold,
                        onClick = { onSignalFilterSelected(SignalType.HOLD) }
                    )
                }
            }

            // Section Header
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "MARKET TICKERS & SIGNALS (${filteredTickers.size})",
                        color = TextMuted,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 1.sp
                    )
                    Text(
                        text = "Tap for charts & breakdown",
                        color = TextMuted,
                        fontSize = 10.5.sp
                    )
                }
            }

            // Tickers list
            items(filteredTickers, key = { it.symbol }) { ticker ->
                val signal = state.signals[ticker.symbol]
                TickerCard(
                    ticker = ticker,
                    signal = signal,
                    onClick = { onSelectTicker(ticker) }
                )
                Spacer(modifier = Modifier.height(10.dp))
            }
        }
    }
}

@Composable
private fun StrategyHeaderCard(
    params: StrategyParameters,
    buyCount: Int,
    sellCount: Int,
    holdCount: Int,
    paperTradesCount: Int,
    totalPnl: Double,
    onTuneClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        colors = CardDefaults.cardColors(containerColor = TerminalSurface),
        border = androidx.compose.foundation.BorderStroke(1.dp, TerminalCardBorder),
        shape = RoundedCornerShape(14.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "CONFLUENCE STRATEGY ENGINE",
                        color = SignalBuy,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 1.sp
                    )
                    Text(
                        text = params.strategyMode.title,
                        color = TextPrimary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = TerminalSurfaceVariant,
                    modifier = Modifier
                        .border(1.dp, TerminalCardBorder, RoundedCornerShape(8.dp))
                        .clickable(onClick = onTuneClick)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Tune, contentDescription = null, tint = SignalBuy, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Parameters", color = TextPrimary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Parameter badge chips
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                ParamBadge(
                    label = "RSI",
                    value = "${params.rsiPeriod} (${params.rsiOversold.toInt()}/${params.rsiOverbought.toInt()})",
                    color = IndicatorRsi
                )
                ParamBadge(
                    label = "MACD",
                    value = "${params.macdFastPeriod}/${params.macdSlowPeriod}/${params.macdSignalPeriod}",
                    color = IndicatorMacd
                )
                ParamBadge(
                    label = "MA",
                    value = "${params.maFastPeriod}/${params.maSlowPeriod} ${params.maType.name}",
                    color = IndicatorMaFast
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Live Signal Counts Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(TerminalSurfaceVariant)
                    .padding(horizontal = 10.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    SignalCountBadge(count = buyCount, label = "BUY", color = SignalBuy)
                    Spacer(modifier = Modifier.width(12.dp))
                    SignalCountBadge(count = sellCount, label = "SELL", color = SignalSell)
                    Spacer(modifier = Modifier.width(12.dp))
                    SignalCountBadge(count = holdCount, label = "HOLD", color = SignalHold)
                }

                if (paperTradesCount > 0) {
                    val isProfit = totalPnl >= 0
                    val sign = if (isProfit) "+" else ""
                    Text(
                        text = "P&L: $sign$${String.format(Locale.US, "%.1f", totalPnl)}",
                        color = if (isProfit) SignalBuy else SignalSell,
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
private fun ParamBadge(label: String, value: String, color: Color) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(TerminalSurfaceVariant)
            .border(0.8.dp, color.copy(alpha = 0.5f), RoundedCornerShape(6.dp))
            .padding(horizontal = 7.dp, vertical = 4.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(text = "$label ", color = color, fontSize = 10.5.sp, fontWeight = FontWeight.Bold)
            Text(text = value, color = TextPrimary, fontSize = 10.5.sp, fontWeight = FontWeight.Medium)
        }
    }
}

@Composable
private fun SignalCountBadge(count: Int, label: String, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(6.dp)
                .clip(CircleShape)
                .background(color)
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = "$count $label",
            color = color,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun SignalFilterPill(
    label: String,
    isSelected: Boolean,
    color: Color,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(if (isSelected) color.copy(alpha = 0.2f) else TerminalSurface)
            .border(
                1.dp,
                if (isSelected) color else TerminalCardBorder,
                RoundedCornerShape(20.dp)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 4.dp)
    ) {
        Text(
            text = label,
            color = if (isSelected) color else TextSecondary,
            fontSize = 11.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
        )
    }
}

@Composable
private fun TickerCard(
    ticker: Ticker,
    signal: TradingSignal?,
    onClick: () -> Unit
) {
    val isPositive = ticker.isPositive
    val changeColor = if (isPositive) SignalBuy else SignalSell
    val sign = if (isPositive) "+" else ""

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .testTag("ticker_card_${ticker.symbol.replace("/", "_")}")
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = TerminalSurface),
        border = androidx.compose.foundation.BorderStroke(1.dp, TerminalCardBorder),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            // Top row: Symbol + Price + Signal Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column {
                        Text(
                            text = ticker.symbol,
                            color = TextPrimary,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                        Text(
                            text = ticker.name,
                            color = TextSecondary,
                            fontSize = 11.sp
                        )
                    }
                }

                // Sparkline
                SparklineChart(
                    prices = ticker.history.takeLast(24).map { it.close },
                    isPositive = isPositive,
                    modifier = Modifier
                        .width(70.dp)
                        .height(30.dp)
                )

                // Price & 24h Change
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = String.format(Locale.US, "$%,.2f", ticker.currentPrice),
                        color = TextPrimary,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "$sign${String.format(Locale.US, "%.2f", ticker.change24hPercent)}%",
                        color = changeColor,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Bottom row: Confluence Signal Badge & Indicator breakdown chips
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (signal != null) {
                    SignalBadge(
                        signalType = signal.signalType,
                        confidence = signal.confidence
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        IndicatorStatusChip(
                            name = "RSI",
                            value = String.format(Locale.US, "%.0f", signal.diagnosis.rsiValue),
                            signal = signal.diagnosis.rsiSignal
                        )
                        IndicatorStatusChip(
                            name = "MACD",
                            value = if (signal.diagnosis.macdHistogram >= 0) "+Hist" else "-Hist",
                            signal = signal.diagnosis.macdSignal
                        )
                        IndicatorStatusChip(
                            name = "MA",
                            value = if (signal.diagnosis.fastMA > signal.diagnosis.slowMA) "Bull" else "Bear",
                            signal = signal.diagnosis.maSignal
                        )
                    }
                }
            }
        }
    }
}
