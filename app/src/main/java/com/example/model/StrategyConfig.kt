package com.example.model

/**
 * Type of Moving Average calculation to use.
 */
enum class MAType(val label: String) {
    EMA("Exponential (EMA)"),
    SMA("Simple (SMA)")
}

/**
 * Strategy signal aggregation mode.
 */
enum class StrategyMode(val title: String, val description: String) {
    CONFLUENCE_WEIGHTED(
        "Confluence Weighted",
        "Scores RSI, MACD, and MA crossover with dynamic confidence weighting"
    ),
    MAJORITY_VOTE(
        "Majority Vote (2 of 3)",
        "Triggers BUY/SELL when at least 2 indicators agree, otherwise HOLD"
    ),
    STRICT_UNANIMOUS(
        "Strict Unanimous (3 of 3)",
        "Requires complete agreement across RSI, MACD, and MA crossover"
    )
}

/**
 * Adjustable indicator thresholds and lookback periods.
 */
data class StrategyParameters(
    // RSI Parameters
    val rsiPeriod: Int = 14,
    val rsiOversold: Double = 30.0,
    val rsiOverbought: Double = 70.0,

    // MACD Parameters
    val macdFastPeriod: Int = 12,
    val macdSlowPeriod: Int = 26,
    val macdSignalPeriod: Int = 9,

    // Moving Average Crossover Parameters
    val maFastPeriod: Int = 9,
    val maSlowPeriod: Int = 21,
    val maType: MAType = MAType.EMA,

    // Aggregation Settings
    val strategyMode: StrategyMode = StrategyMode.CONFLUENCE_WEIGHTED,
    val minConfidenceThreshold: Int = 60
)

/**
 * Ready-made strategy presets for quick tuning.
 */
enum class StrategyPreset(
    val title: String,
    val description: String,
    val params: StrategyParameters
) {
    BALANCED_SWING(
        title = "Balanced Swing",
        description = "Standard 14-period RSI (30/70), 12/26/9 MACD, and 9/21 EMA crossover.",
        params = StrategyParameters(
            rsiPeriod = 14,
            rsiOversold = 30.0,
            rsiOverbought = 70.0,
            macdFastPeriod = 12,
            macdSlowPeriod = 26,
            macdSignalPeriod = 9,
            maFastPeriod = 9,
            maSlowPeriod = 21,
            maType = MAType.EMA,
            strategyMode = StrategyMode.CONFLUENCE_WEIGHTED,
            minConfidenceThreshold = 60
        )
    ),
    AGGRESSIVE_SCALP(
        title = "Aggressive Scalp",
        description = "Fast 7-period RSI (25/75), 6/13/5 MACD, and 5/13 EMA crossover for quick signals.",
        params = StrategyParameters(
            rsiPeriod = 7,
            rsiOversold = 25.0,
            rsiOverbought = 75.0,
            macdFastPeriod = 6,
            macdSlowPeriod = 13,
            macdSignalPeriod = 5,
            maFastPeriod = 5,
            maSlowPeriod = 13,
            maType = MAType.EMA,
            strategyMode = StrategyMode.MAJORITY_VOTE,
            minConfidenceThreshold = 55
        )
    ),
    CONSERVATIVE_TREND(
        title = "Conservative Trend",
        description = "Smoother 21-period RSI (35/65), 19/39/9 MACD, and 20/50 SMA for macro trends.",
        params = StrategyParameters(
            rsiPeriod = 21,
            rsiOversold = 35.0,
            rsiOverbought = 65.0,
            macdFastPeriod = 19,
            macdSlowPeriod = 39,
            macdSignalPeriod = 9,
            maFastPeriod = 20,
            maSlowPeriod = 50,
            maType = MAType.SMA,
            strategyMode = StrategyMode.STRICT_UNANIMOUS,
            minConfidenceThreshold = 70
        )
    )
}
