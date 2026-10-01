package com.example.antitheft

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.LocationManager
import android.os.Build
import android.os.IBinder
import android.os.Looper
import android.provider.Settings
import android.telephony.SmsManager
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority

class LocationService : Service() {

    companion object {
        const val EXTRA_REPLY_NUMBER = "extra_reply_number"
        const val ACTION_START_TRACKING = "com.example.antitheft.action.START_TRACKING"
        const val ACTION_STOP_TRACKING = "com.example.antitheft.action.STOP_TRACKING"

        private const val CHANNEL_ID = "antitheft_channel"
        private const val NOTIFICATION_ID = 1
        private const val UPDATE_INTERVAL_MS = 60_000L
    }

    private var locationCallback: LocationCallback? = null
    private var replyNumber: String? = null
    private var isTracking = false

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForeground(NOTIFICATION_ID, buildNotification("در حال آماده‌سازی ردیابی..."))

        when (intent?.action) {
            ACTION_STOP_TRACKING -> {
                stopTracking()
                stopSelf()
                return START_NOT_STICKY
            }
            ACTION_START_TRACKING -> {
                val number = intent.getStringExtra(EXTRA_REPLY_NUMBER)
                if (number.isNullOrEmpty()) {
                    stopSelf()
                    return START_NOT_STICKY
                }
                replyNumber = number
                ensureLocationEnabled()
                startTracking(number)
            }
            else -> {
                stopSelf()
                return START_NOT_STICKY
            }
        }

        return START_REDELIVER_INTENT
    }

    private fun startTracking(number: String) {
        if (isTracking) return
        isTracking = true

        val hasFineLocation = ActivityCompat.checkSelfPermission(
            this, android.Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        if (!hasFineLocation) {
            sendSms(number, "مجوز موقعیت مکانی داده نشده؛ ردیابی ممکن نیست.")
            stopSelf()
            return
        }

        val client = LocationServices.getFusedLocationProviderClient(this)
        val request = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, UPDATE_INTERVAL_MS)
    .setMinUpdateIntervalMillis(UPDATE_INTERVAL_MS)
    .setWaitForAccurateLocation(true)
    .build()
        locationCallback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                val location = result.lastLocation ?: return
                val mapsLink = "https://maps.google.com/?q=${location.latitude},${location.longitude}"
                sendSms(number, "موقعیت گوشی: $mapsLink")
                updateNotification("آخرین ارسال: همین الان — برای توقف، پیامک «توقف» بفرستید.")
            }
        }

        try {
            client.requestLocationUpdates(request, locationCallback as LocationCallback, Looper.getMainLooper())
            updateNotification("ردیابی فعال — ارسال موقعیت هر 1 دقیقه.")
        } catch (_: SecurityException) {
            sendSms(number, "خطای مجوز در دریافت موقعیت.")
            stopSelf()
        }
    }

    private fun stopTracking() {
        if (!isTracking) return
        isTracking = false

        val client = LocationServices.getFusedLocationProviderClient(this)
        locationCallback?.let { client.removeLocationUpdates(it) }
        locationCallback = null

        replyNumber?.let { sendSms(it, "ردیابی موقعیت متوقف شد.") }
    }

    override fun onDestroy() {
        stopTracking()
        super.onDestroy()
    }

    private fun ensureLocationEnabled() {
        val locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
        val isEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) ||
            locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)

        if (isEnabled) return

        try {
            val settingsIntent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            startActivity(settingsIntent)
        } catch (_: Exception) {
        }
    }

    private fun sendSms(number: String, message: String) {
        try {
            val smsManager = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                getSystemService(SmsManager::class.java)
            } else {
                @Suppress("DEPRECATION")
                SmsManager.getDefault()
            }
            val parts = smsManager.divideMessage(message)
            smsManager.sendMultipartTextMessage(number, null, parts, null, null)
        } catch (_: Exception) {
        }
    }

    private fun buildNotification(text: String): Notification {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "AntiTheft Location Service",
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("ردیابی موقعیت")
            .setContentText(text)
            .setSmallIcon(android.R.drawable.ic_menu_mylocation)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .build()
    }

    private fun updateNotification(text: String) {
        val manager = getSystemService(NotificationManager::class.java)
        manager.notify(NOTIFICATION_ID, buildNotification(text))
    }
}
