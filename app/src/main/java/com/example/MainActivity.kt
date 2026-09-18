package com.example

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.model.Ticker
import com.example.notification.SignalNotificationHelper
import com.example.ui.StrategySettingsSheet
import com.example.ui.TickerDetailScreen
import com.example.ui.TradingDashboardScreen
import com.example.ui.theme.TerminalBackground
import com.example.ui.theme.TradingBotTheme
import com.example.viewmodel.TradingBotViewModel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.util.Locale

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Initialize system notification channel for trading signal alerts
        SignalNotificationHelper.createNotificationChannel(this)

        val initialTickerSymbol = intent?.getStringExtra("selected_ticker_symbol")

        setContent {
            TradingBotTheme {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(TerminalBackground)
                        .safeDrawingPadding()
                ) {
                    TradingBotApp(initialTickerSymbol = initialTickerSymbol)
                }
            }
        }
    }
}

@Composable
fun TradingBotApp(
    viewModel: TradingBotViewModel = viewModel(),
    initialTickerSymbol: String? = null
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()
    var detailTicker by remember { mutableStateOf<Ticker?>(null) }
    var isStrategySheetOpen by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    // Request POST_NOTIFICATIONS permission on Android 13+
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { _ -> }

    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val hasPermission = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            if (!hasPermission) {
                permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    // Handle deep link / notification tap navigation
    LaunchedEffect(initialTickerSymbol, uiState.tickers) {
        if (initialTickerSymbol != null && detailTicker == null) {
            uiState.tickers.find { it.symbol == initialTickerSymbol }?.let {
                detailTicker = it
                viewModel.selectTicker(it)
            }
        }
    }

    // Listen to AI-generated BUY and SELL signal alerts for push notifications and snackbar
    LaunchedEffect(Unit) {
        viewModel.signalAlertFlow.collectLatest { alert ->
            // Trigger push notification on device
            SignalNotificationHelper.showSignalNotification(context, alert)

            // Display in-app alert snackbar
            val priceStr = String.format(Locale.US, "$%,.2f", alert.price)
            val message = "🚨 AI ${alert.signalType.name} Alert: ${alert.symbol} at $priceStr (${alert.confidence}% conf)"
            val result = snackbarHostState.showSnackbar(
                message = message,
                actionLabel = "View ${alert.symbol}",
                duration = SnackbarDuration.Short
            )
            if (result == SnackbarResult.ActionPerformed) {
                val targetTicker = uiState.tickers.find { it.symbol == alert.symbol }
                if (targetTicker != null) {
                    detailTicker = targetTicker
                    viewModel.selectTicker(targetTicker)
                }
            }
        }
    }

    // Synchronize detail ticker if live market updates occur
    val activeTicker = detailTicker?.let { dt ->
        uiState.tickers.find { it.symbol == dt.symbol } ?: dt
    }

    Scaffold(
        snackbarHost = {
            SnackbarHost(
                hostState = snackbarHostState,
                modifier = Modifier.testTag("signal_snackbar_host")
            )
        },
        containerColor = TerminalBackground
    ) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
            if (activeTicker != null) {
                BackHandler {
                    detailTicker = null
                }
                TickerDetailScreen(
                    ticker = activeTicker,
                    signal = uiState.signals[activeTicker.symbol],
                    strategyParams = uiState.strategyParams,
                    paperTrades = uiState.paperTrades,
                    onBack = { detailTicker = null },
                    onOpenTuneSettings = { isStrategySheetOpen = true },
                    onExecuteTrade = { ticker, signal ->
                        viewModel.executePaperTrade(ticker, signal)
                    },
                    onCloseTrade = { tradeId ->
                        viewModel.closePaperTrade(tradeId)
                    }
                )
            } else {
                TradingDashboardScreen(
                    state = uiState,
                    onSelectTicker = { ticker ->
                        detailTicker = ticker
                        viewModel.selectTicker(ticker)
                    },
                    onOpenTuneSettings = { isStrategySheetOpen = true },
                    onCategorySelected = { category ->
                        viewModel.setCategoryFilter(category)
                    },
                    onSignalFilterSelected = { signalType ->
                        viewModel.setSignalFilter(signalType)
                    },
                    onSearchQueryChanged = { query ->
                        viewModel.setSearchQuery(query)
                    },
                    onToggleLiveStreaming = {
                        viewModel.toggleLiveStreaming()
                    },
                    onDismissAlert = {
                        viewModel.dismissActiveAlert()
                    },
                    onTriggerTestAlert = {
                        val sym = activeTicker?.symbol ?: uiState.tickers.firstOrNull()?.symbol ?: "BTC"
                        viewModel.triggerTestSignalAlert(sym)
                    }
                )
            }
        }
    }

    if (isStrategySheetOpen) {
        StrategySettingsSheet(
            currentParams = uiState.strategyParams,
            onSave = { newParams ->
                viewModel.updateStrategyParameters(newParams)
            },
            onDismiss = { isStrategySheetOpen = false }
        )
    }
}

