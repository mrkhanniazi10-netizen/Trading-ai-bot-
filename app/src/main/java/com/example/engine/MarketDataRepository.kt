package com.example.engine

import com.example.model.Candle
import com.example.model.MarketCategory
import com.example.model.Ticker
import kotlin.math.abs
import kotlin.random.Random

/**
 * Provides market ticker data, realistic historical candle generation,
 * and simulated live price feeds.
 */
object MarketDataRepository {

    private val tickerSeeds = listOf(
        TickerSeed("BTC/USD", "Bitcoin", MarketCategory.CRYPTO, 64850.0, 0.025, 0.038, 12000.0),
        TickerSeed("ETH/USD", "Ethereum", MarketCategory.CRYPTO, 3450.0, 0.018, 0.042, 45000.0),
        TickerSeed("SOL/USD", "Solana", MarketCategory.CRYPTO, 152.40, -0.012, 0.055, 180000.0),
        TickerSeed("NVDA", "NVIDIA Corp.", MarketCategory.TECH, 128.60, 0.034, 0.028, 650000.0),
        TickerSeed("AAPL", "Apple Inc.", MarketCategory.TECH, 226.40, 0.008, 0.014, 420000.0),
        TickerSeed("TSLA", "Tesla Inc.", MarketCategory.TECH, 254.20, -0.022, 0.035, 380000.0),
        TickerSeed("MSFT", "Microsoft Corp.", MarketCategory.TECH, 438.10, 0.012, 0.015, 290000.0),
        TickerSeed("SPY", "SPDR S&P 500 ETF", MarketCategory.INDICES, 564.80, 0.006, 0.009, 890000.0),
        TickerSeed("QQQ", "Invesco QQQ Trust", MarketCategory.INDICES, 485.30, 0.011, 0.012, 540000.0),
        TickerSeed("AMD", "Adv. Micro Devices", MarketCategory.TECH, 158.70, -0.015, 0.031, 310000.0)
    )

    private data class TickerSeed(
        val symbol: String,
        val name: String,
        val category: MarketCategory,
        val basePrice: Double,
        val initialTrend: Double,
        val volatility: Double,
        val baseVolume: Double
    )

    /**
     * Initializes default tickers with 80 realistic historical candles each.
     */
    fun getInitialTickers(): List<Ticker> {
        return tickerSeeds.map { seed ->
            val candles = generateHistoricalCandles(
                seed.basePrice,
                seed.volatility,
                seed.initialTrend,
                seed.baseVolume,
                count = 80
            )
            val currentPrice = candles.last().close
            val firstPrice = candles.first().open
            val changePercent = ((currentPrice - firstPrice) / firstPrice) * 100.0
            val high24h = candles.takeLast(24).maxOf { it.high }
            val low24h = candles.takeLast(24).minOf { it.low }
            val volume24h = candles.takeLast(24).sumOf { it.volume }

            Ticker(
                symbol = seed.symbol,
                name = seed.name,
                category = seed.category,
                currentPrice = currentPrice,
                change24hPercent = changePercent,
                high24h = high24h,
                low24h = low24h,
                volume24h = volume24h,
                history = candles
            )
        }
    }

    /**
     * Generates a realistic candlestick series with trending segments, cyclic pullbacks, and volatility.
     */
    private fun generateHistoricalCandles(
        basePrice: Double,
        volatility: Double,
        trendBias: Double,
        baseVolume: Double,
        count: Int
    ): List<Candle> {
        val candles = ArrayList<Candle>(count)
        val now = System.currentTimeMillis()
        val candleDuration = 15 * 60 * 1000L // 15-minute candles
        val startTime = now - (count * candleDuration)

        var currentPrice = basePrice * (1.0 - (trendBias * 0.5))
        val random = Random(basePrice.hashCode())

        var momentum = trendBias

        for (i in 0 until count) {
            val candleTime = startTime + (i * candleDuration)

            // Random shock plus slight mean reversion
            val shock = (random.nextDouble() - 0.48) * volatility
            momentum = (momentum * 0.85) + shock

            val open = currentPrice
            val close = open * (1.0 + momentum)
            val spread = abs(close - open)
            val highWick = spread * random.nextDouble(0.2, 1.2)
            val lowWick = spread * random.nextDouble(0.2, 1.2)

            val high = maxOf(open, close) + highWick
            val low = minOf(open, close) - lowWick
            val volume = baseVolume * random.nextDouble(0.6, 2.1)

            candles.add(
                Candle(
                    timestamp = candleTime,
                    open = open,
                    high = high,
                    low = low,
                    close = close,
                    volume = volume
                )
            )

            currentPrice = close
        }

        return candles
    }

    /**
     * Advances market state by 1 tick or candle bar.
     */
    fun simulateTick(ticker: Ticker): Ticker {
        val random = Random.Default
        val last = ticker.history.last()
        val percentChange = (random.nextDouble() - 0.49) * 0.008
        val newClose = last.close * (1.0 + percentChange)
        val newHigh = maxOf(last.high, newClose)
        val newLow = minOf(last.low, newClose)
        val newVolume = last.volume + random.nextDouble(10.0, 50.0)

        val updatedLastCandle = last.copy(
            close = newClose,
            high = newHigh,
            low = newLow,
            volume = newVolume
        )

        val updatedHistory = ticker.history.dropLast(1) + updatedLastCandle
        val firstPrice = updatedHistory.first().open
        val newChangePercent = ((newClose - firstPrice) / firstPrice) * 100.0

        return ticker.copy(
            currentPrice = newClose,
            change24hPercent = newChangePercent,
            history = updatedHistory
        )
    }
}
