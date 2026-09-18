package com.example.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.MainActivity
import com.example.R
import com.example.model.SignalAlert
import com.example.model.SignalType
import java.util.Locale

object SignalNotificationHelper {

    private const val CHANNEL_ID = "trading_signals_channel"
    private const val CHANNEL_NAME = "Trading Bot Signals"
    private const val CHANNEL_DESC = "Real-time AI BUY and SELL signal alerts"

    fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(CHANNEL_ID, CHANNEL_NAME, importance).apply {
                description = CHANNEL_DESC
                enableVibration(true)
            }
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            notificationManager?.createNotificationChannel(channel)
        }
    }

    fun showSignalNotification(context: Context, alert: SignalAlert) {
        // Only trigger for BUY or SELL signals
        if (alert.signalType == SignalType.HOLD) return

        // Check POST_NOTIFICATIONS permission on Android 13+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    context,
                    android.Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                // Permission not yet granted, in-app snackbar/banner handles the alert
                return
            }
        }

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("selected_ticker_symbol", alert.symbol)
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            alert.symbol.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val title = "⚡ AI ${alert.signalType.name} Signal: ${alert.symbol} (${alert.confidence}%)"
        val priceFormatted = String.format(Locale.US, "$%,.2f", alert.price)
        val text = "${alert.tickerName} at $priceFormatted — ${alert.summary}"

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)

        try {
            with(NotificationManagerCompat.from(context)) {
                notify(alert.symbol.hashCode(), builder.build())
            }
        } catch (_: SecurityException) {
            // Handled safely if permissions revoke at runtime
        }
    }
}
