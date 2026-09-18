package com.example.model

/**
 * Primary trading action signal.
 */
enum class SignalType(val label: String) {
    BUY("BUY"),
    SELL("SELL"),
    HOLD("HOLD")
}

/**
 * Conviction strength of the trading signal.
 */
enum class SignalStrength(val label: String) {
    STRONG("Strong"),
    MODERATE("Moderate"),
    WEAK("Weak")
}

/**
 * Detailed diagnostic readout for each indicator component.
 */
data class IndicatorDiagnosis(
    // RSI
    val rsiValue: Double,
    val rsiSignal: SignalType,
    val rsiReason: String,
    val isRsiOversold: Boolean,
    val isRsiOverbought: Boolean,

    // MACD
    val macdLine: Double,
    val macdSignalLine: Double,
    val macdHistogram: Double,
    val macdSignal: SignalType,
    val macdReason: String,
    val isMacdBullishCross: Boolean,
    val isMacdBearishCross: Boolean,

    // Moving Average Crossover
    val fastMA: Double,
    val slowMA: Double,
    val maSignal: SignalType,
    val maReason: String,
    val isGoldenCross: Boolean,
    val isDeathCross: Boolean
)

/**
 * Comprehensive signal computed for a specific ticker and time point.
 */
data class TradingSignal(
    val tickerSymbol: String,
    val signalType: SignalType,
    val strength: SignalStrength,
    val confidence: Int,              // 0 - 100%
    val summary: String,
    val diagnosis: IndicatorDiagnosis,
    val timestamp: Long,
    val priceAtSignal: Double
)

/**
 * Simulated paper trade position.
 */
data class PaperTrade(
    val id: String,
    val symbol: String,
    val type: SignalType,
    val entryPrice: Double,
    val currentPrice: Double,
    val pnlAmount: Double,
    val pnlPercent: Double,
    val timestamp: Long,
    val status: String = "OPEN"
)
