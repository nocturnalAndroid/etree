package com.timelapse.app

import android.Manifest
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.net.Uri
import android.net.wifi.WifiManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.timelapse.app.databinding.ActivityMainBinding
import java.net.InetAddress

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var prefs: SharedPreferences

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.all { it.value }
        if (allGranted) updatePermissionStatus()
        else binding.tvPermissionStatus.text = getString(R.string.permissions_denied)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        prefs = getSharedPreferences(App.PREFS_NAME, MODE_PRIVATE)
        binding.etNtfyTopic.setText(prefs.getString(App.PREF_NTFY_TOPIC, ""))
        binding.etDriveAccount.setText(prefs.getString(App.PREF_DRIVE_ACCOUNT, ""))

        binding.btnRequestPermissions.setOnClickListener { requestRequiredPermissions() }
        binding.btnStartListener.setOnClickListener { savePrefsAndStartListener() }

        updatePermissionStatus()
        showLocalAddress()
    }

    private fun requestRequiredPermissions() {
        val required = buildList {
            add(Manifest.permission.CAMERA)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
                add(Manifest.permission.POST_NOTIFICATIONS)
        }
        val missing = required.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }
        if (missing.isNotEmpty()) permissionLauncher.launch(missing.toTypedArray())
        else updatePermissionStatus()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val pm = getSystemService(POWER_SERVICE) as android.os.PowerManager
            if (!pm.isIgnoringBatteryOptimizations(packageName)) {
                startActivity(Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                    data = Uri.parse("package:$packageName")
                })
            }
        }
    }

    private fun updatePermissionStatus() {
        val cameraGranted = ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        val notifGranted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
        else true

        if (cameraGranted && notifGranted) {
            binding.tvPermissionStatus.text = getString(R.string.permissions_granted)
        } else {
            val missing = buildList {
                if (!cameraGranted) add("Camera")
                if (!notifGranted) add("Notifications")
            }
            binding.tvPermissionStatus.text = getString(R.string.permissions_missing, missing.joinToString(", "))
        }
    }

    private fun savePrefsAndStartListener() {
        prefs.edit()
            .putString(App.PREF_NTFY_TOPIC, binding.etNtfyTopic.text.toString().trim())
            .putString(App.PREF_DRIVE_ACCOUNT, binding.etDriveAccount.text.toString().trim())
            .apply()
        startForegroundService(Intent(this, ListenerService::class.java))
        binding.tvStatus.text = getString(R.string.listener_running, getLocalIpAddress(), 8080)
        binding.btnStartListener.isEnabled = false
        binding.btnStartListener.text = getString(R.string.btn_listener_started)
    }

    private fun showLocalAddress() {
        val ip = getLocalIpAddress()
        if (ip != "127.0.0.1") binding.tvStatus.text = getString(R.string.web_ui_address, ip, 8080)
    }

    private fun getLocalIpAddress(): String {
        return try {
            val wifiManager = applicationContext.getSystemService(WIFI_SERVICE) as WifiManager
            val ipInt = wifiManager.connectionInfo.ipAddress
            if (ipInt != 0) InetAddress.getByAddress(byteArrayOf(
                (ipInt and 0xff).toByte(), (ipInt shr 8 and 0xff).toByte(),
                (ipInt shr 16 and 0xff).toByte(), (ipInt shr 24 and 0xff).toByte()
            )).hostAddress ?: "127.0.0.1"
            else "127.0.0.1"
        } catch (e: Exception) { "127.0.0.1" }
    }
}
