package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.engine.IndicatorCalculator
import com.example.engine.MarketDataRepository
import com.example.engine.SignalGenerationEngine
import com.example.model.MAType
import com.example.model.SignalAlert
import com.example.model.SignalType
import com.example.model.StrategyParameters
import com.example.notification.SignalNotificationHelper
import com.example.viewmodel.TradingBotViewModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("Trading AI Bot", appName)
  }

  @Test
  fun `verify RSI calculation bounds`() {
    val prices = listOf(100.0, 102.0, 101.0, 105.0, 104.0, 107.0, 110.0, 108.0, 112.0, 115.0)
    val rsi = IndicatorCalculator.calculateRSI(prices, period = 5)
    assertTrue("RSI list should match prices size", rsi.size == prices.size)
    val lastRsi = rsi.last()
    assertNotNull("Last RSI should not be null", lastRsi)
    assertTrue("RSI must be between 0 and 100", lastRsi!! in 0.0..100.0)
  }

  @Test
  fun `verify MACD calculation outputs`() {
    val prices = (1..50).map { 100.0 + (it * 0.5) }
    val macd = IndicatorCalculator.calculateMACD(prices, fastPeriod = 12, slowPeriod = 26, signalPeriod = 9)
    assertEquals(prices.size, macd.size)
    val lastMacd = macd.last()
    assertNotNull("Last MACD point should exist for 50 bars", lastMacd)
    assertTrue("Histogram equals MACD minus Signal", 
      kotlin.math.abs(lastMacd!!.histogram - (lastMacd.macd - lastMacd.signal)) < 0.0001
    )
  }

  @Test
  fun `verify SignalGenerationEngine produces signals for all tickers`() {
    val tickers = MarketDataRepository.getInitialTickers()
    val params = StrategyParameters()
    tickers.forEach { ticker ->
      val signal = SignalGenerationEngine.evaluateSignal(ticker.symbol, ticker.history, params)
      assertNotNull(signal)
      assertTrue("Signal confidence should be between 0 and 100", signal.confidence in 0..100)
      assertTrue("SignalType should be valid", signal.signalType in setOf(SignalType.BUY, SignalType.SELL, SignalType.HOLD))
    }
  }

  @Test
  fun `verify SignalNotificationHelper creates channel and handles alerts safely`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    SignalNotificationHelper.createNotificationChannel(context)

    val buyAlert = SignalAlert(
      symbol = "BTC",
      tickerName = "Bitcoin",
      signalType = SignalType.BUY,
      confidence = 88,
      price = 68420.0,
      summary = "RSI oversold rebound and MACD bullish cross detected."
    )

    SignalNotificationHelper.showSignalNotification(context, buyAlert)

    val holdAlert = buyAlert.copy(signalType = SignalType.HOLD)
    // Should safely ignore HOLD
    SignalNotificationHelper.showSignalNotification(context, holdAlert)
  }

  @Test
  fun `verify ViewModel alert dispatch and dismiss`() {
    val viewModel = TradingBotViewModel()
    val initialAlert = viewModel.uiState.value.activeAlert

    viewModel.triggerTestSignalAlert("ETH/USD")
    val triggeredAlert = viewModel.uiState.value.activeAlert
    assertNotNull("Active alert should be populated after trigger", triggeredAlert)
    assertEquals("ETH/USD", triggeredAlert?.symbol)
    assertTrue("Alert history should record the new alert", viewModel.uiState.value.alertHistory.isNotEmpty())

    viewModel.dismissActiveAlert()
    assertEquals(null, viewModel.uiState.value.activeAlert)
  }
}

