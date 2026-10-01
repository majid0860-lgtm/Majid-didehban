package com.example.antitheft

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

/**
 * صفحه‌ی اصلی: دریافت و ذخیره‌ی دو شماره
 * - trusted_sender: شماره‌ای که اجازه دارد با پیامک "موقعیت" درخواست لوکیشن بدهد
 * - location_receiver: شماره‌ای که لینک لوکیشن به آن پیامک می‌شود
 */
class MainActivity : AppCompatActivity() {

    companion object {
        const val PREFS_NAME = "antitheft_prefs"
        const val KEY_TRUSTED_SENDER = "trusted_sender"
        const val KEY_LOCATION_RECEIVER = "location_receiver"
        private const val PERMISSION_REQUEST_CODE = 100
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

        val editSender = findViewById<EditText>(R.id.editTrustedSender)
        val editReceiver = findViewById<EditText>(R.id.editLocationReceiver)
        val btnSave = findViewById<Button>(R.id.btnSave)
        val txtStatus = findViewById<TextView>(R.id.txtStatus)

        editSender.setText(prefs.getString(KEY_TRUSTED_SENDER, ""))
        editReceiver.setText(prefs.getString(KEY_LOCATION_RECEIVER, ""))

        btnSave.setOnClickListener {
            val sender = editSender.text.toString().trim()
            val receiver = editReceiver.text.toString().trim()

            if (sender.isEmpty() || receiver.isEmpty()) {
                Toast.makeText(this, "هر دو شماره را وارد کنید", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            prefs.edit()
                .putString(KEY_TRUSTED_SENDER, sender)
                .putString(KEY_LOCATION_RECEIVER, receiver)
                .apply()

            Toast.makeText(this, "ذخیره شد", Toast.LENGTH_SHORT).show()
        }

        requestNeededPermissions()
        txtStatus.text = "برای فعال‌سازی کامل، همه‌ی مجوزها را تایید کنید و بهینه‌سازی باتری را برای این اپ خاموش کنید."
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
        }
    }
}
