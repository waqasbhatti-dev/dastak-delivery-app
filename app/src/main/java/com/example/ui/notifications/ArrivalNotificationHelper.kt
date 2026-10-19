package com.example.ui.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.MainActivity
import com.example.R
import com.example.data.model.Order
import com.example.localization.AppLanguage
import java.util.Locale

object ArrivalNotificationHelper {

    const val CHANNEL_ID = "dastak_rider_arrival_channel"
    private const val CHANNEL_NAME = "Dastak Rider Arrival Alerts"
    private const val CHANNEL_DESC = "Notifications and audio alerts when your rider arrives outside your doorstep"

    fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            val audioAttributes = AudioAttributes.Builder()
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .setUsage(AudioAttributes.USAGE_NOTIFICATION_COMMUNICATION_INSTANT)
                .build()

            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = CHANNEL_DESC
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 500, 200, 500)
                setSound(soundUri, audioAttributes)
                lockscreenVisibility = NotificationCompat.VISIBILITY_PUBLIC
            }

            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            manager?.createNotificationChannel(channel)
        }
    }

    fun buildAnnouncementText(order: Order, language: AppLanguage): String {
        val totalFormatted = String.format(Locale.ENGLISH, "%.2f", order.totalAmount)
        return if (language == AppLanguage.URDU) {
            "آپ کا رائیڈر آپ کے گھر کے باہر پہنچ چکا ہے۔ براہِ کرم باہر آ کر اپنا آرڈر وصول کر لیں۔ آپ کے آرڈر کی کل رقم $totalFormatted روپے ہے۔"
        } else {
            "Your rider has arrived outside your home. Please come outside and collect your order. Your approved order total is PKR $totalFormatted."
        }
    }

    fun showArrivalNotification(context: Context, order: Order, language: AppLanguage) {
        createNotificationChannel(context)

        val title = if (language == AppLanguage.URDU) {
            "دستک: رائیڈر گھر کے باہر پہنچ چکا ہے! 🛵"
        } else {
            "Dastak: Rider Arrived Outside! 🛵"
        }

        val message = buildAnnouncementText(order, language)

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("notification_order_id", order.id)
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            order.id.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_CALL)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setVibrate(longArrayOf(0, 500, 200, 500))
            .build()

        try {
            val manager = NotificationManagerCompat.from(context)
            if (manager.areNotificationsEnabled()) {
                manager.notify(order.id.hashCode(), notification)
            }
        } catch (_: SecurityException) {
            // Handled gracefully when permission is not granted
        }
    }
}
