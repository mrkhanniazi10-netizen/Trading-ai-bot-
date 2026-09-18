package com.example.model

import java.util.UUID

/**
 * Represents an in-app and system notification event when the AI trading engine
 * detects a new BUY or SELL signal.
 */
data class SignalAlert(
    val id: String = UUID.randomUUID().toString(),
    val symbol: String,
    val tickerName: String,
    val signalType: SignalType,
    val confidence: Int,
    val price: Double,
    val summary: String,
    val timestamp: Long = System.currentTimeMillis()
)
