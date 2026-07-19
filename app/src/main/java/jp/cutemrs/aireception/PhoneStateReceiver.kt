package jp.cutemrs.aireception

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.telephony.TelephonyManager
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat

class PhoneStateReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != TelephonyManager.ACTION_PHONE_STATE_CHANGED) return
        val state = intent.getStringExtra(TelephonyManager.EXTRA_STATE) ?: return
        if (state != TelephonyManager.EXTRA_STATE_RINGING) return

        val canRead = ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CALL_LOG) == PackageManager.PERMISSION_GRANTED
        val number = if (canRead) {
            intent.getStringExtra(TelephonyManager.EXTRA_INCOMING_NUMBER).orEmpty().filter(Char::isDigit)
        } else ""

        if (number.isNotBlank()) {
            context.getSharedPreferences("ai_reception", Context.MODE_PRIVATE)
                .edit().putString("last_phone", number).apply()
        }
        showIncomingNotification(context, number)
    }

    private fun showIncomingNotification(context: Context, phone: String) {
        val channelId = "incoming_calls"
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= 26) {
            manager.createNotificationChannel(
                NotificationChannel(channelId, "AI受付 着信", NotificationManager.IMPORTANCE_HIGH)
            )
        }

        val uri = android.net.Uri.parse("aireception://incoming?phone=$phone")
        val openIntent = Intent(Intent.ACTION_VIEW, uri, context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context, 1001, openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val label = if (phone.isBlank()) "着信を受信しました" else "着信: $phone"
        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(android.R.drawable.sym_action_call)
            .setContentTitle("AI受付 Pro")
            .setContentText(label)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_CALL)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()
        manager.notify(1001, notification)
    }
}
