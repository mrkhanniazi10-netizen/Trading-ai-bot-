package com.example.engine

import com.example.model.MAType
import kotlin.math.max

data class MACDPoint(
    val macd: Double,
    val signal: Double,
    val histogram: Double
)

data class IndicatorSeries(
    val prices: List<Double>,
    val rsi: List<Double?>,
    val macd: List<MACDPoint?>,
    val fastMA: List<Double?>,
    val slowMA: List<Double?>
)

/**
 * Mathematical calculation engine for trading technical indicators.
 */
object IndicatorCalculator {

    /**
     * Simple Moving Average (SMA).
     */
    fun calculateSMA(prices: List<Double>, period: Int): List<Double?> {
        if (prices.isEmpty() || period <= 0) return emptyList()
        val result = ArrayList<Double?>(prices.size)
        var windowSum = 0.0

        for (i in prices.indices) {
            windowSum += prices[i]
            if (i >= period) {
                windowSum -= prices[i - period]
            }
            if (i >= period - 1) {
                result.add(windowSum / period)
            } else {
                result.add(null)
            }
        }
        return result
    }

    /**
     * Exponential Moving Average (EMA).
     */
    fun calculateEMA(prices: List<Double>, period: Int): List<Double?> {
        if (prices.isEmpty() || period <= 0) return emptyList()
        val result = ArrayList<Double?>(prices.size)
        val multiplier = 2.0 / (period + 1.0)

        // Seed with the first period's SMA
        var seedSum = 0.0
        var prevEma: Double? = null

        for (i in prices.indices) {
            if (i < period - 1) {
                seedSum += prices[i]
                result.add(null)
            } else if (i == period - 1) {
                seedSum += prices[i]
                val initialSma = seedSum / period
                prevEma = initialSma
                result.add(initialSma)
            } else {
                val currentEma = (prices[i] - prevEma!!) * multiplier + prevEma
                prevEma = currentEma
                result.add(currentEma)
            }
        }
        return result
    }

    /**
     * Moving Average calculation based on MAType enum.
     */
    fun calculateMA(prices: List<Double>, period: Int, type: MAType): List<Double?> {
        return when (type) {
            MAType.SMA -> calculateSMA(prices, period)
            MAType.EMA -> calculateEMA(prices, period)
        }
    }

    /**
     * Relative Strength Index (RSI) using Wilder's smoothed average.
     */
    fun calculateRSI(prices: List<Double>, period: Int): List<Double?> {
        if (prices.size <= period || period <= 0) {
            return List(prices.size) { null }
        }

        val result = ArrayList<Double?>(prices.size)
        // First candle has no delta
        result.add(null)

        var avgGain = 0.0
        var avgLoss = 0.0

        // Initial sum of gains and losses for first `period` deltas
        for (i in 1..period) {
            val delta = prices[i] - prices[i - 1]
            if (delta > 0) avgGain += delta else avgLoss += -delta
            result.add(null)
        }

        avgGain /= period
        avgLoss /= period

        // First valid RSI at index `period`
        val initialRs = if (avgLoss == 0.0) 100.0 else avgGain / avgLoss
        val initialRsi = if (avgLoss == 0.0) 100.0 else 100.0 - (100.0 / (1.0 + initialRs))
        result[period] = initialRsi.coerceIn(0.0, 100.0)

        // Subsequent smoothed RSI
        for (i in (period + 1) until prices.size) {
            val delta = prices[i] - prices[i - 1]
            val currentGain = if (delta > 0) delta else 0.0
            val currentLoss = if (delta < 0) -delta else 0.0

            avgGain = (avgGain * (period - 1) + currentGain) / period
            avgLoss = (avgLoss * (period - 1) + currentLoss) / period

            val rsi = when {
                avgLoss == 0.0 && avgGain == 0.0 -> 50.0
                avgLoss == 0.0 -> 100.0
                else -> {
                    val rs = avgGain / avgLoss
                    100.0 - (100.0 / (1.0 + rs))
                }
            }
            result.add(rsi.coerceIn(0.0, 100.0))
        }

        return result
    }

    /**
     * Moving Average Convergence Divergence (MACD).
     * MACD Line = Fast EMA - Slow EMA
     * Signal Line = Signal Period EMA of MACD Line
     * Histogram = MACD Line - Signal Line
     */
    fun calculateMACD(
        prices: List<Double>,
        fastPeriod: Int,
        slowPeriod: Int,
        signalPeriod: Int
    ): List<MACDPoint?> {
        val size = prices.size
        if (size < slowPeriod) return List(size) { null }

        val actualFast = minOf(fastPeriod, slowPeriod)
        val actualSlow = max(fastPeriod, slowPeriod)

        val fastEma = calculateEMA(prices, actualFast)
        val slowEma = calculateEMA(prices, actualSlow)

        // Calculate MACD Line
        val macdLine = ArrayList<Double?>(size)
        for (i in 0 until size) {
            val fastVal = fastEma.getOrNull(i)
            val slowVal = slowEma.getOrNull(i)
            if (fastVal != null && slowVal != null) {
                macdLine.add(fastVal - slowVal)
            } else {
                macdLine.add(null)
            }
        }

        // Calculate Signal Line (EMA of MACD line where valid)
        val firstValidIndex = macdLine.indexOfFirst { it != null }
        if (firstValidIndex == -1 || (size - firstValidIndex) < signalPeriod) {
            return List(size) { null }
        }

        val nonNullMacd = macdLine.subList(firstValidIndex, size).filterNotNull()
        val signalEmaSub = calculateEMA(nonNullMacd, signalPeriod)

        val result = ArrayList<MACDPoint?>(size)
        for (i in 0 until size) {
            if (i < firstValidIndex) {
                result.add(null)
            } else {
                val subIdx = i - firstValidIndex
                val macdVal = nonNullMacd.getOrNull(subIdx)
                val signalVal = signalEmaSub.getOrNull(subIdx)

                if (macdVal != null && signalVal != null) {
                    result.add(
                        MACDPoint(
                            macd = macdVal,
                            signal = signalVal,
                            histogram = macdVal - signalVal
                        )
                    )
                } else {
                    result.add(null)
                }
            }
        }

        return result
    }

    /**
     * Calculates all indicator series for a given close price list.
     */
    fun computeAll(
        prices: List<Double>,
        rsiPeriod: Int,
        macdFast: Int,
        macdSlow: Int,
        macdSignal: Int,
        maFast: Int,
        maSlow: Int,
        maType: MAType
    ): IndicatorSeries {
        return IndicatorSeries(
            prices = prices,
            rsi = calculateRSI(prices, rsiPeriod),
            macd = calculateMACD(prices, macdFast, macdSlow, macdSignal),
            fastMA = calculateMA(prices, maFast, maType),
            slowMA = calculateMA(prices, maSlow, maType)
        )
    }
}
