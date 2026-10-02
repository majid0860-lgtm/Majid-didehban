package com.example.antitheft

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

class MainActivity : AppCompatActivity() {

    companion object {
        const val PREFS_NAME = "antitheft_prefs"
        const val KEY_TRUSTED_SENDER = "trusted_sender"
        const val KEY_LOCATION_RECEIVER = "location_receiver"
        const val KEY_CONSENT_GIVEN = "consent_given"
        private const val PERMISSION_REQUEST_CODE = 100
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

        val editSender = findViewById<EditText>(R.id.editTrustedSender)
        val editReceiver = findViewById<EditText>(R.id.editLocationReceiver)
        val checkConsent = findViewById<CheckBox>(R.id.checkConsent)
        val btnSave = findViewById<Button>(R.id.btnSave)
        val txtStatus = findViewById<TextView>(R.id.txtStatus)

        editSender.setText(prefs.getString(KEY_TRUSTED_SENDER, ""))
        editReceiver.setText(prefs.getString(KEY_LOCATION_RECEIVER, ""))
        checkConsent.isChecked = prefs.getBoolean(KEY_CONSENT_GIVEN, false)

        btnSave.setOnClickListener {
            val sender = editSender.text.toString().trim()
            val receiver = editReceiver.text.toString().trim()

            if (sender.isEmpty() || receiver.isEmpty()) {
                Toast.makeText(this, "هر دو شماره را وارد کنید", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (!checkConsent.isChecked) {
                Toast.makeText(
                    this,
                    "برای فعال‌سازی باید تایید رضایت صاحب گوشی را بزنید",
                    Toast.LENGTH_LONG
                ).show()
                return@setOnClickListener
            }

            prefs.edit()
                .putString(KEY_TRUSTED_SENDER, sender)
                .putString(KEY_LOCATION_RECEIVER, receiver)
                .putBoolean(KEY_CONSENT_GIVEN, true)
                .apply()

            startAlwaysOnNotification()

            Toast.makeText(this, "ذخیره شد — نوتیفیکیشن دائمی شفافیت فعال شد", Toast.LENGTH_LONG).show()
        }

        if (checkConsent.isChecked) {
            startAlwaysOnNotification()
        }

        requestNeededPermissions()
        txtStatus.text = "برای فعال‌سازی کامل، همه‌ی مجوزها و دیالوگ‌های بعدی را تایید کنید."
    }

    private fun startAlwaysOnNotification() {
        val serviceIntent = Intent(this, AlwaysOnNotificationService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            ContextCompat.startForegroundService(this, serviceIntent)
        } else {
            startService(serviceIntent)
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERMISSION_REQUEST_CODE) {
            requestIgnoreBatteryOptimizations()
        }
    }

    @Suppress("BatteryLife")
    private fun requestIgnoreBatteryOptimizations() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) return

        val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
        val alreadyIgnoring = powerManager.isIgnoringBatteryOptimizations(packageName)

        if (!alreadyIgnoring) {
            try {
                val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                    data = Uri.parse("package:$packageName")
                }
                startActivity(intent)
            } catch (_: Exception) {
            }
        }
    }

    private fun requestNeededPermissions() {
        val permissions = mutableListOf(
            Manifest.permission.RECEIVE_SMS,
            Manifest.permission.SEND_SMS,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            permissions.add(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions.add(Manifest.permission.POST_NOTIFICATIONS)
        }

        val notGranted = permissions.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }

        if (notGranted.isNotEmpty()) {
            ActivityCompat.requestPermissions(this, notGranted.toTypedArray(), PERMISSION_REQUEST_CODE)
        } else {
            requestIgnoreBatteryOptimizations()
        }
    }
}
