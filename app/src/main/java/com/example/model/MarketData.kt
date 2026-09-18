package com.example.model

/**
 * Represents a single candlestick bar of market data.
 */
data class Candle(
    val timestamp: Long,
    val open: Double,
    val high: Double,
    val low: Double,
    val close: Double,
    val volume: Double
) {
    val isBullish: Boolean get() = close >= open
}

/**
 * Supported market categories.
 */
enum class MarketCategory(val label: String) {
    ALL("All Assets"),
    CRYPTO("Crypto"),
    TECH("Tech Stocks"),
    INDICES("Indices & Commodities")
}

/**
 * Represents a market ticker with price history and current stats.
 */
data class Ticker(
    val symbol: String,
    val name: String,
    val category: MarketCategory,
    val currentPrice: Double,
    val change24hPercent: Double,
    val high24h: Double,
    val low24h: Double,
    val volume24h: Double,
    val history: List<Candle>
) {
    val isPositive: Boolean get() = change24hPercent >= 0.0
}
