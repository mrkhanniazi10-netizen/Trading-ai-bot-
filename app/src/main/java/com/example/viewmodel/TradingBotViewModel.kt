package com.example.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.engine.MarketDataRepository
import com.example.engine.SignalGenerationEngine
import com.example.model.MarketCategory
import com.example.model.PaperTrade
import com.example.model.SignalAlert
import com.example.model.SignalType
import com.example.model.StrategyParameters
import com.example.model.StrategyPreset
import com.example.model.Ticker
import com.example.model.TradingSignal
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.UUID

data class DashboardUiState(
    val tickers: List<Ticker> = emptyList(),
    val signals: Map<String, TradingSignal> = emptyMap(),
    val selectedTicker: Ticker? = null,
    val selectedSignal: TradingSignal? = null,
    val strategyParams: StrategyParameters = StrategyParameters(),
    val selectedCategory: MarketCategory = MarketCategory.ALL,
    val selectedSignalFilter: SignalType? = null,
    val searchQuery: String = "",
    val isLiveStreaming: Boolean = true,
    val paperTrades: List<PaperTrade> = emptyList(),
    val paperBalance: Double = 50000.0,
    val totalProfitLoss: Double = 0.0,
    val winRatePercent: Double = 0.0,
    val activeAlert: SignalAlert? = null,
    val alertHistory: List<SignalAlert> = emptyList()
)

class TradingBotViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(DashboardUiState())
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    private val _signalAlertFlow = MutableSharedFlow<SignalAlert>(extraBufferCapacity = 10)
    val signalAlertFlow: SharedFlow<SignalAlert> = _signalAlertFlow.asSharedFlow()

    private var streamJob: Job? = null
    // Keep track of the last alerted signal type per symbol to alert on changes
    private val lastAlertedSignalTypes = mutableMapOf<String, SignalType>()

    init {
        loadInitialData()
        startLiveStreaming()
    }

    private fun loadInitialData() {
        val initialTickers = MarketDataRepository.getInitialTickers()
        val currentParams = _uiState.value.strategyParams

        val computedSignals = initialTickers.associate { ticker ->
            val sig = SignalGenerationEngine.evaluateSignal(ticker.symbol, ticker.history, currentParams)
            lastAlertedSignalTypes[ticker.symbol] = sig.signalType
            ticker.symbol to sig
        }

        val firstTicker = initialTickers.firstOrNull()

        _uiState.update { state ->
            state.copy(
                tickers = initialTickers,
                signals = computedSignals,
                selectedTicker = firstTicker,
                selectedSignal = firstTicker?.let { computedSignals[it.symbol] }
            )
        }
    }

    fun selectTicker(ticker: Ticker) {
        val currentParams = _uiState.value.strategyParams
        val signal = SignalGenerationEngine.evaluateSignal(ticker.symbol, ticker.history, currentParams)
        _uiState.update { it.copy(selectedTicker = ticker, selectedSignal = signal) }
    }

    fun updateStrategyParameters(newParams: StrategyParameters) {
        _uiState.update { state ->
            val updatedSignals = state.tickers.associate { ticker ->
                ticker.symbol to SignalGenerationEngine.evaluateSignal(ticker.symbol, ticker.history, newParams)
            }
            val updatedSelectedSignal = state.selectedTicker?.let { updatedSignals[it.symbol] }
            state.copy(
                strategyParams = newParams,
                signals = updatedSignals,
                selectedSignal = updatedSelectedSignal
            )
        }
    }

    fun applyPreset(preset: StrategyPreset) {
        updateStrategyParameters(preset.params)
    }

    fun setCategoryFilter(category: MarketCategory) {
        _uiState.update { it.copy(selectedCategory = category) }
    }

    fun setSignalFilter(signalType: SignalType?) {
        _uiState.update { it.copy(selectedSignalFilter = signalType) }
    }

    fun setSearchQuery(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
    }

    fun toggleLiveStreaming() {
        val isStreaming = !_uiState.value.isLiveStreaming
        _uiState.update { it.copy(isLiveStreaming = isStreaming) }
        if (isStreaming) {
            startLiveStreaming()
        } else {
            streamJob?.cancel()
        }
    }

    private fun startLiveStreaming() {
        streamJob?.cancel()
        streamJob = viewModelScope.launch {
            while (isActive) {
                delay(3000L) // Advance every 3 seconds for active feel
                if (!_uiState.value.isLiveStreaming) break

                val currentState = _uiState.value
                val updatedTickers = currentState.tickers.map { ticker ->
                    MarketDataRepository.simulateTick(ticker)
                }

                val updatedSignals = updatedTickers.associate { ticker ->
                    ticker.symbol to SignalGenerationEngine.evaluateSignal(
                        ticker.symbol,
                        ticker.history,
                        currentState.strategyParams
                    )
                }

                // Check for new BUY or SELL signals generated by the AI model
                var newestAlert: SignalAlert? = null
                for (ticker in updatedTickers) {
                    val newSig = updatedSignals[ticker.symbol] ?: continue
                    val prevType = lastAlertedSignalTypes[ticker.symbol]
                    if (newSig.signalType != SignalType.HOLD && newSig.signalType != prevType) {
                        lastAlertedSignalTypes[ticker.symbol] = newSig.signalType
                        val alert = SignalAlert(
                            symbol = ticker.symbol,
                            tickerName = ticker.name,
                            signalType = newSig.signalType,
                            confidence = newSig.confidence,
                            price = ticker.currentPrice,
                            summary = newSig.summary
                        )
                        newestAlert = alert
                        _signalAlertFlow.tryEmit(alert)
                    } else if (newSig.signalType == SignalType.HOLD) {
                        // Reset if it transitioned to HOLD so next BUY/SELL triggers
                        lastAlertedSignalTypes[ticker.symbol] = SignalType.HOLD
                    }
                }

                val updatedSelectedTicker = currentState.selectedTicker?.let { sel ->
                    updatedTickers.find { it.symbol == sel.symbol } ?: sel
                }

                val updatedSelectedSignal = updatedSelectedTicker?.let {
                    updatedSignals[it.symbol]
                }

                // Update open paper trades
                val currentPrices = updatedTickers.associate { it.symbol to it.currentPrice }
                val updatedTrades = currentState.paperTrades.map { trade ->
                    val curPrice = currentPrices[trade.symbol] ?: trade.currentPrice
                    val pnlAmount = if (trade.type == SignalType.BUY) {
                        (curPrice - trade.entryPrice)
                    } else {
                        (trade.entryPrice - curPrice)
                    }
                    val pnlPercent = (pnlAmount / trade.entryPrice) * 100.0
                    trade.copy(
                        currentPrice = curPrice,
                        pnlAmount = pnlAmount,
                        pnlPercent = pnlPercent
                    )
                }

                val totalPnl = updatedTrades.sumOf { it.pnlAmount }
                val winningTrades = updatedTrades.count { it.pnlAmount > 0 }
                val winRate = if (updatedTrades.isNotEmpty()) {
                    (winningTrades.toDouble() / updatedTrades.size) * 100.0
                } else 0.0

                _uiState.update { state ->
                    val activeAlert = newestAlert ?: state.activeAlert
                    val updatedHistory = if (newestAlert != null) {
                        (listOf(newestAlert) + state.alertHistory).take(30)
                    } else {
                        state.alertHistory
                    }

                    state.copy(
                        tickers = updatedTickers,
                        signals = updatedSignals,
                        selectedTicker = updatedSelectedTicker,
                        selectedSignal = updatedSelectedSignal,
                        paperTrades = updatedTrades,
                        totalProfitLoss = totalPnl,
                        winRatePercent = winRate,
                        activeAlert = activeAlert,
                        alertHistory = updatedHistory
                    )
                }
            }
        }
    }

    fun dismissActiveAlert() {
        _uiState.update { it.copy(activeAlert = null) }
    }

    fun triggerTestSignalAlert(symbol: String) {
        val ticker = _uiState.value.tickers.find { 
            it.symbol.equals(symbol, ignoreCase = true) || it.symbol.startsWith(symbol, ignoreCase = true)
        } ?: return
        val signal = _uiState.value.signals[ticker.symbol]
        val alertType = if (signal?.signalType == SignalType.BUY) SignalType.BUY else if (signal?.signalType == SignalType.SELL) SignalType.SELL else SignalType.BUY
        val testAlert = SignalAlert(
            symbol = ticker.symbol,
            tickerName = ticker.name,
            signalType = alertType,
            confidence = signal?.confidence ?: 85,
            price = ticker.currentPrice,
            summary = signal?.summary ?: "High-confidence ${alertType.name} signal triggered by momentum surge."
        )
        lastAlertedSignalTypes[ticker.symbol] = alertType
        _signalAlertFlow.tryEmit(testAlert)
        _uiState.update { state ->
            state.copy(
                activeAlert = testAlert,
                alertHistory = (listOf(testAlert) + state.alertHistory).take(30)
            )
        }
    }

    fun executePaperTrade(ticker: Ticker, signal: TradingSignal) {
        if (signal.signalType == SignalType.HOLD) return

        val newTrade = PaperTrade(
            id = UUID.randomUUID().toString(),
            symbol = ticker.symbol,
            type = signal.signalType,
            entryPrice = ticker.currentPrice,
            currentPrice = ticker.currentPrice,
            pnlAmount = 0.0,
            pnlPercent = 0.0,
            timestamp = System.currentTimeMillis()
        )

        _uiState.update { state ->
            val updatedTrades = listOf(newTrade) + state.paperTrades
            state.copy(paperTrades = updatedTrades)
        }
    }

    fun closePaperTrade(tradeId: String) {
        _uiState.update { state ->
            val target = state.paperTrades.find { it.id == tradeId } ?: return@update state
            val remainingTrades = state.paperTrades.filterNot { it.id == tradeId }
            state.copy(
                paperTrades = remainingTrades,
                paperBalance = state.paperBalance + target.pnlAmount
            )
        }
    }

    override fun onCleared() {
        super.onCleared()
        streamJob?.cancel()
    }
}
