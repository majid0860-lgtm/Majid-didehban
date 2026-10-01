package com.example.antitheft

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Telephony
import androidx.core.content.ContextCompat

/**
 * پیامک‌های ورودی را می‌گیرد. اگر فرستنده همان "شماره‌ی مجاز" ذخیره‌شده باشد
 * و متن پیامک برابر با کلمه‌ی کلیدی "موقعیت" باشد، سرویس موقعیت‌یابی را استارت می‌کند.
 */
class SmsReceiver : BroadcastReceiver() {

    private val startKeyword = "موقعیت"
    private val stopKeyword = "توقف"

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Telephony.Sms.Intents.SMS_RECEIVED_ACTION) return

        val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent)
        if (messages.isNullOrEmpty()) return

        val prefs = context.getSharedPreferences(MainActivity.PREFS_NAME, Context.MODE_PRIVATE)
        val trustedSender = prefs.getString(MainActivity.KEY_TRUSTED_SENDER, null) ?: return
        val locationReceiver = prefs.getString(MainActivity.KEY_LOCATION_RECEIVER, null) ?: return

        for (sms in messages) {
            val sender = sms.originatingAddress ?: continue
            val body = sms.messageBody?.trim() ?: continue

            if (normalizeNumber(sender) != normalizeNumber(trustedSender)) continue

            when {
                body.equals(startKeyword, ignoreCase = true) -> {
                    val serviceIntent = Intent(context, LocationService::class.java).apply {
                        action = LocationService.ACTION_START_TRACKING
                        putExtra(LocationService.EXTRA_REPLY_NUMBER, locationReceiver)
                    }
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        ContextCompat.startForegroundService(context, serviceIntent)
                    } else {
                        context.startService(serviceIntent)
                    }
                }
                body.equals(stopKeyword, ignoreCase = true) -> {
                    val serviceIntent = Intent(context, LocationService::class.java).apply {
                        action = LocationService.ACTION_STOP_TRACKING
                    }
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        ContextCompat.startForegroundService(context, serviceIntent)
                    } else {
                        context.startService(serviceIntent)
                    }
                }
            }
        }
    }

    /** مقایسه‌ی ساده‌ی شماره‌ها با نادیده گرفتن فاصله، خط تیره و کد کشور صفر/پلاس */
    private fun normalizeNumber(number: String): String {
        val digitsOnly = number.filter { it.isDigit() }
        return if (digitsOnly.length > 10) digitsOnly.takeLast(10) else digitsOnly
    }
}
