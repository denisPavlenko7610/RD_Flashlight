package com.rdragon.rd_flashlightautoshutdown

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.os.Build
import android.os.CountDownTimer
import androidx.core.app.NotificationCompat

class FlashService : Service() {
    private lateinit var cameraManager: CameraManager
    private var cameraId: String = ""
    private lateinit var devicePolicyManager: DevicePolicyManager
    private lateinit var compName: ComponentName
    private var timer: CountDownTimer? = null

    override fun onCreate() {
        super.onCreate()
        cameraManager = getSystemService(Context.CAMERA_SERVICE) as CameraManager
        cameraId = cameraManager.cameraIdList.first {
            cameraManager.getCameraCharacteristics(it)
                .get(CameraCharacteristics.FLASH_INFO_AVAILABLE) == true
        }
        devicePolicyManager = getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
        compName = ComponentName(this, MyDeviceAdminReceiver::class.java)
        startForeground(1, createNotification("Фонарик включен"))
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val seconds = intent?.getLongExtra("duration", 0L) ?: 0L
        turnFlashlight(true)
        timer?.cancel()
        timer = object : CountDownTimer(seconds * 1000, 1000) {
            override fun onTick(ms: Long) { /* можно обновлять статус */ }
            override fun onFinish() {
                turnFlashlight(false)
                if (devicePolicyManager.isAdminActive(compName)) {
                    devicePolicyManager.lockNow()
                }
                stopSelf()
            }
        }.start()
        return START_STICKY
    }

    private fun turnFlashlight(state: Boolean) {
        cameraManager.setTorchMode(cameraId, state)
    }

    private fun createNotification(text: String): Notification {
        val chanId = "flash_service_channel"
        val mgr = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val chan = NotificationChannel(chanId, "Flashlight Service",
                NotificationManager.IMPORTANCE_LOW).apply {
                description = "Канал для работы таймера фонарика"
            }
            mgr.createNotificationChannel(chan)
        }
        return NotificationCompat.Builder(this, chanId)
            .setContentText(text)
            .setSmallIcon(R.drawable.flashlight)
            .build()
    }

    override fun onBind(intent: Intent?) = null
    override fun onDestroy() { timer?.cancel(); turnFlashlight(false); super.onDestroy() }
}