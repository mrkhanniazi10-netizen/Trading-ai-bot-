package com.example.engine

import com.example.model.Candle
import com.example.model.IndicatorDiagnosis
import com.example.model.SignalStrength
import com.example.model.SignalType
import com.example.model.StrategyMode
import com.example.model.StrategyParameters
import com.example.model.TradingSignal
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * Advanced Trading Signal Generation Strategy Engine.
 * Confluence evaluation of RSI, MACD, and Moving Average crossovers.
 */
object SignalGenerationEngine {

    /**
     * Evaluates full trading signal for a given ticker candle history and strategy parameters.
     */
    fun evaluateSignal(
        symbol: String,
        candles: List<Candle>,
        params: StrategyParameters
    ): TradingSignal {
        if (candles.size < 2) {
            return createDefaultHoldSignal(symbol, candles.lastOrNull()?.close ?: 0.0)
        }

        val prices = candles.map { it.close }
        val series = IndicatorCalculator.computeAll(
            prices = prices,
            rsiPeriod = params.rsiPeriod,
            macdFast = params.macdFastPeriod,
            macdSlow = params.macdSlowPeriod,
            macdSignal = params.macdSignalPeriod,
            maFast = params.maFastPeriod,
            maSlow = params.maSlowPeriod,
            maType = params.maType
        )

        val lastIdx = prices.lastIndex
        val prevIdx = (lastIdx - 1).coerceAtLeast(0)

        // 1. Diagnose RSI
        val currentRsi = series.rsi.getOrNull(lastIdx) ?: 50.0
        val prevRsi = series.rsi.getOrNull(prevIdx) ?: 50.0
        val isRsiOversold = currentRsi <= params.rsiOversold
        val isRsiOverbought = currentRsi >= params.rsiOverbought
        val rsiRecoveringFromOversold = prevRsi <= params.rsiOversold && currentRsi > params.rsiOversold
        val rsiFallingFromOverbought = prevRsi >= params.rsiOverbought && currentRsi < params.rsiOverbought

        val (rsiSignal, rsiReason) = when {
            isRsiOversold -> {
                SignalType.BUY to String.format(Locale.US, "Oversold at %.1f (below %.0f threshold)", currentRsi, params.rsiOversold)
            }
            rsiRecoveringFromOversold -> {
                SignalType.BUY to String.format(Locale.US, "Rebounding out of oversold (%.1f -> %.1f)", prevRsi, currentRsi)
            }
            isRsiOverbought -> {
                SignalType.SELL to String.format(Locale.US, "Overbought at %.1f (above %.0f threshold)", currentRsi, params.rsiOverbought)
            }
            rsiFallingFromOverbought -> {
                SignalType.SELL to String.format(Locale.US, "Dropping from overbought (%.1f -> %.1f)", prevRsi, currentRsi)
            }
            currentRsi > 55.0 -> {
                SignalType.BUY to String.format(Locale.US, "Bullish momentum (%.1f)", currentRsi)
            }
            currentRsi < 45.0 -> {
                SignalType.SELL to String.format(Locale.US, "Bearish momentum (%.1f)", currentRsi)
            }
            else -> {
                SignalType.HOLD to String.format(Locale.US, "Neutral momentum at %.1f", currentRsi)
            }
        }

        // 2. Diagnose MACD
        val currentMacd = series.macd.getOrNull(lastIdx)
        val prevMacd = series.macd.getOrNull(prevIdx)
        val macdLine = currentMacd?.macd ?: 0.0
        val macdSignalLine = currentMacd?.signal ?: 0.0
        val macdHist = currentMacd?.histogram ?: 0.0
        val prevHist = prevMacd?.histogram ?: 0.0

        val isMacdBullishCross = (prevHist <= 0.0 && macdHist > 0.0) ||
                (prevMacd != null && prevMacd.macd <= prevMacd.signal && macdLine > macdSignalLine)
        val isMacdBearishCross = (prevHist >= 0.0 && macdHist < 0.0) ||
                (prevMacd != null && prevMacd.macd >= prevMacd.signal && macdLine < macdSignalLine)

        val (macdSignal, macdReason) = when {
            isMacdBullishCross -> {
                SignalType.BUY to String.format(Locale.US, "Bullish Crossover: MACD crossed above Signal (hist: %+.2f)", macdHist)
            }
            isMacdBearishCross -> {
                SignalType.SELL to String.format(Locale.US, "Bearish Crossover: MACD dropped below Signal (hist: %+.2f)", macdHist)
            }
            macdHist > 0.0 && macdHist >= prevHist -> {
                SignalType.BUY to String.format(Locale.US, "Bullish expansion (MACD > Signal, hist %+.2f)", macdHist)
            }
            macdHist < 0.0 && macdHist <= prevHist -> {
                SignalType.SELL to String.format(Locale.US, "Bearish expansion (MACD < Signal, hist %+.2f)", macdHist)
            }
            macdHist > 0.0 -> {
                SignalType.HOLD to String.format(Locale.US, "Positive MACD but momentum decelerating")
            }
            else -> {
                SignalType.HOLD to String.format(Locale.US, "Negative MACD with slowing bearish pressure")
            }
        }

        // 3. Diagnose Moving Average Crossover
        val currentFastMA = series.fastMA.getOrNull(lastIdx) ?: prices.last()
        val currentSlowMA = series.slowMA.getOrNull(lastIdx) ?: prices.last()
        val prevFastMA = series.fastMA.getOrNull(prevIdx) ?: prices.getOrNull(prevIdx) ?: prices.last()
        val prevSlowMA = series.slowMA.getOrNull(prevIdx) ?: prices.getOrNull(prevIdx) ?: prices.last()

        val isGoldenCross = prevFastMA <= prevSlowMA && currentFastMA > currentSlowMA
        val isDeathCross = prevFastMA >= prevSlowMA && currentFastMA < currentSlowMA
        val isFastAboveSlow = currentFastMA > currentSlowMA

        val maTypeName = params.maType.name
        val (maSignal, maReason) = when {
            isGoldenCross -> {
                SignalType.BUY to "Golden Cross: Fast $maTypeName (${params.maFastPeriod}) crossed above Slow $maTypeName (${params.maSlowPeriod})"
            }
            isDeathCross -> {
                SignalType.SELL to "Death Cross: Fast $maTypeName (${params.maFastPeriod}) dropped below Slow $maTypeName (${params.maSlowPeriod})"
            }
            isFastAboveSlow -> {
                SignalType.BUY to "Bullish Trend: Fast $maTypeName above Slow $maTypeName"
            }
            else -> {
                SignalType.SELL to "Bearish Trend: Fast $maTypeName below Slow $maTypeName"
            }
        }

        val diagnosis = IndicatorDiagnosis(
            rsiValue = currentRsi,
            rsiSignal = rsiSignal,
            rsiReason = rsiReason,
            isRsiOversold = isRsiOversold,
            isRsiOverbought = isRsiOverbought,
            macdLine = macdLine,
            macdSignalLine = macdSignalLine,
            macdHistogram = macdHist,
            macdSignal = macdSignal,
            macdReason = macdReason,
            isMacdBullishCross = isMacdBullishCross,
            isMacdBearishCross = isMacdBearishCross,
            fastMA = currentFastMA,
            slowMA = currentSlowMA,
            maSignal = maSignal,
            maReason = maReason,
            isGoldenCross = isGoldenCross,
            isDeathCross = isDeathCross
        )

        // 4. Combine Signals according to Strategy Mode
        return when (params.strategyMode) {
            StrategyMode.CONFLUENCE_WEIGHTED -> calculateWeightedConfluence(symbol, candles.last(), diagnosis, params)
            StrategyMode.MAJORITY_VOTE -> calculateMajorityVote(symbol, candles.last(), diagnosis, params)
            StrategyMode.STRICT_UNANIMOUS -> calculateStrictUnanimous(symbol, candles.last(), diagnosis, params)
        }
    }

    private fun calculateWeightedConfluence(
        symbol: String,
        lastCandle: Candle,
        diagnosis: IndicatorDiagnosis,
        params: StrategyParameters
    ): TradingSignal {
        var score = 0 // Range from -100 to +100

        // RSI Score (Weight: 35 max)
        when {
            diagnosis.isRsiOversold -> score += 35
            diagnosis.rsiSignal == SignalType.BUY -> score += 20
            diagnosis.isRsiOverbought -> score -= 35
            diagnosis.rsiSignal == SignalType.SELL -> score -= 20
        }

        // MACD Score (Weight: 35 max)
        when {
            diagnosis.isMacdBullishCross -> score += 35
            diagnosis.macdSignal == SignalType.BUY -> score += 20
            diagnosis.isMacdBearishCross -> score -= 35
            diagnosis.macdSignal == SignalType.SELL -> score -= 20
        }

        // MA Crossover Score (Weight: 30 max)
        when {
            diagnosis.isGoldenCross -> score += 30
            diagnosis.maSignal == SignalType.BUY -> score += 15
            diagnosis.isDeathCross -> score -= 30
            diagnosis.maSignal == SignalType.SELL -> score -= 15
        }

        val absScore = abs(score)
        val confidence = absScore.coerceIn(20, 98)

        val signalType: SignalType
        val strength: SignalStrength
        val summary: String

        if (score >= 35 && confidence >= params.minConfidenceThreshold) {
            signalType = SignalType.BUY
            strength = if (score >= 70) SignalStrength.STRONG else SignalStrength.MODERATE
            summary = buildConfluenceSummary(SignalType.BUY, strength, diagnosis)
        } else if (score <= -35 && confidence >= params.minConfidenceThreshold) {
            signalType = SignalType.SELL
            strength = if (score <= -70) SignalStrength.STRONG else SignalStrength.MODERATE
            summary = buildConfluenceSummary(SignalType.SELL, strength, diagnosis)
        } else {
            signalType = SignalType.HOLD
            strength = SignalStrength.WEAK
            summary = "Signals are mixed or below threshold (${params.minConfidenceThreshold}%). Strategy advises maintaining current stance."
        }

        return TradingSignal(
            tickerSymbol = symbol,
            signalType = signalType,
            strength = strength,
            confidence = confidence,
            summary = summary,
            diagnosis = diagnosis,
            timestamp = lastCandle.timestamp,
            priceAtSignal = lastCandle.close
        )
    }

    private fun calculateMajorityVote(
        symbol: String,
        lastCandle: Candle,
        diagnosis: IndicatorDiagnosis,
        params: StrategyParameters
    ): TradingSignal {
        val signals = listOf(diagnosis.rsiSignal, diagnosis.macdSignal, diagnosis.maSignal)
        val buyCount = signals.count { it == SignalType.BUY }
        val sellCount = signals.count { it == SignalType.SELL }

        val signalType: SignalType
        val strength: SignalStrength
        val confidence: Int
        val summary: String

        when {
            buyCount >= 2 -> {
                signalType = SignalType.BUY
                strength = if (buyCount == 3) SignalStrength.STRONG else SignalStrength.MODERATE
                confidence = if (buyCount == 3) 92 else 72
                summary = "$buyCount of 3 indicators vote BUY (${diagnosis.rsiSignal.name} RSI, ${diagnosis.macdSignal.name} MACD, ${diagnosis.maSignal.name} MA Crossover)."
            }
            sellCount >= 2 -> {
                signalType = SignalType.SELL
                strength = if (sellCount == 3) SignalStrength.STRONG else SignalStrength.MODERATE
                confidence = if (sellCount == 3) 92 else 72
                summary = "$sellCount of 3 indicators vote SELL (${diagnosis.rsiSignal.name} RSI, ${diagnosis.macdSignal.name} MACD, ${diagnosis.maSignal.name} MA Crossover)."
            }
            else -> {
                signalType = SignalType.HOLD
                strength = SignalStrength.WEAK
                confidence = 45
                summary = "Indicator vote split evenly. No majority consensus achieved."
            }
        }

        return TradingSignal(
            tickerSymbol = symbol,
            signalType = signalType,
            strength = strength,
            confidence = confidence,
            summary = summary,
            diagnosis = diagnosis,
            timestamp = lastCandle.timestamp,
            priceAtSignal = lastCandle.close
        )
    }

    private fun calculateStrictUnanimous(
        symbol: String,
        lastCandle: Candle,
        diagnosis: IndicatorDiagnosis,
        params: StrategyParameters
    ): TradingSignal {
        val isAllBuy = diagnosis.rsiSignal == SignalType.BUY &&
                diagnosis.macdSignal == SignalType.BUY &&
                diagnosis.maSignal == SignalType.BUY

        val isAllSell = diagnosis.rsiSignal == SignalType.SELL &&
                diagnosis.macdSignal == SignalType.SELL &&
                diagnosis.maSignal == SignalType.SELL

        val signalType: SignalType
        val strength: SignalStrength
        val confidence: Int
        val summary: String

        when {
            isAllBuy -> {
                signalType = SignalType.BUY
                strength = SignalStrength.STRONG
                confidence = 96
                summary = "Strict Unanimous BUY: RSI, MACD, and MA Crossover all align bullishly."
            }
            isAllSell -> {
                signalType = SignalType.SELL
                strength = SignalStrength.STRONG
                confidence = 96
                summary = "Strict Unanimous SELL: RSI, MACD, and MA Crossover all align bearishly."
            }
            else -> {
                signalType = SignalType.HOLD
                strength = SignalStrength.WEAK
                confidence = 35
                summary = "Waiting for complete 3-way consensus. RSI: ${diagnosis.rsiSignal}, MACD: ${diagnosis.macdSignal}, MA: ${diagnosis.maSignal}."
            }
        }

        return TradingSignal(
            tickerSymbol = symbol,
            signalType = signalType,
            strength = strength,
            confidence = confidence,
            summary = summary,
            diagnosis = diagnosis,
            timestamp = lastCandle.timestamp,
            priceAtSignal = lastCandle.close
        )
    }

    private fun buildConfluenceSummary(
        signal: SignalType,
        strength: SignalStrength,
        diag: IndicatorDiagnosis
    ): String {
        val reasons = mutableListOf<String>()
        if (diag.isRsiOversold || (signal == SignalType.BUY && diag.rsiSignal == SignalType.BUY)) {
            reasons.add(diag.rsiReason)
        } else if (diag.isRsiOverbought || (signal == SignalType.SELL && diag.rsiSignal == SignalType.SELL)) {
            reasons.add(diag.rsiReason)
        }

        if (diag.isMacdBullishCross || diag.isMacdBearishCross || diag.macdSignal == signal) {
            reasons.add(diag.macdReason)
        }

        if (diag.isGoldenCross || diag.isDeathCross || diag.maSignal == signal) {
            reasons.add(diag.maReason)
        }

        val prefix = "${strength.label} $signal signal triggered."
        return if (reasons.isNotEmpty()) {
            "$prefix Drivers: " + reasons.joinToString("; ")
        } else {
            "$prefix Indicators indicate favorable ${signal.label} momentum."
        }
    }

    private fun createDefaultHoldSignal(symbol: String, price: Double): TradingSignal {
        return TradingSignal(
            tickerSymbol = symbol,
            signalType = SignalType.HOLD,
            strength = SignalStrength.WEAK,
            confidence = 50,
            summary = "Insufficient historical data to compute indicators.",
            diagnosis = IndicatorDiagnosis(
                rsiValue = 50.0,
                rsiSignal = SignalType.HOLD,
                rsiReason = "Awaiting data",
                isRsiOversold = false,
                isRsiOverbought = false,
                macdLine = 0.0,
                macdSignalLine = 0.0,
                macdHistogram = 0.0,
                macdSignal = SignalType.HOLD,
                macdReason = "Awaiting data",
                isMacdBullishCross = false,
                isMacdBearishCross = false,
                fastMA = price,
                slowMA = price,
                maSignal = SignalType.HOLD,
                maReason = "Awaiting data",
                isGoldenCross = false,
                isDeathCross = false
            ),
            timestamp = System.currentTimeMillis(),
            priceAtSignal = price
        )
    }

    /**
     * Runs a quick backtest over the candle history to compute historical signals and simulated returns.
     */
    fun backtestSignals(
        candles: List<Candle>,
        params: StrategyParameters
    ): List<TradingSignal> {
        val minLookback = maxOf(params.rsiPeriod, params.macdSlowPeriod + params.macdSignalPeriod, params.maSlowPeriod) + 5
        if (candles.size <= minLookback) return emptyList()

        val results = mutableListOf<TradingSignal>()
        // Evaluate every 3 candles for efficiency in backtest timeline
        val step = maxOf(1, (candles.size - minLookback) / 20)
        for (i in minLookback until candles.size step step) {
            val subCandles = candles.subList(0, i + 1)
            val sig = evaluateSignal("BACKTEST", subCandles, params)
            results.add(sig)
        }
        return results
    }
}
