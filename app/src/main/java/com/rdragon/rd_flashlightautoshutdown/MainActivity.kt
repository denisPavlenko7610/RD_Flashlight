package com.rdragon.rd_flashlightautoshutdown

import android.app.AlarmManager
import android.app.PendingIntent
import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.CountDownTimer
import android.os.SystemClock
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager

class MainActivity : AppCompatActivity() {
    private lateinit var tvCountdown: TextView
    private lateinit var etMin: TextInputEditText
    private lateinit var etSec: TextInputEditText
    private lateinit var btnStart: MaterialButton
    private lateinit var btnStop: MaterialButton
    private var timer: CountDownTimer? = null
    private lateinit var alarmMgr: AlarmManager
    private lateinit var alarmPi: PendingIntent
    private lateinit var cameraManager: CameraManager
    private lateinit var cameraId: String

    override fun onCreate(saved: Bundle?) {
        super.onCreate(saved)
        setContentView(R.layout.activity_main)

        tvCountdown = findViewById(R.id.tvCountdown)
        etMin       = findViewById(R.id.etMinutes)
        etSec       = findViewById(R.id.etSeconds)
        btnStart    = findViewById(R.id.btnStart)
        btnStop     = findViewById(R.id.btnStopService)

        // Камера
        cameraManager = getSystemService(Context.CAMERA_SERVICE) as CameraManager
        cameraId = cameraManager.cameraIdList.first { id ->
            cameraManager.getCameraCharacteristics(id)
                .get(CameraCharacteristics.FLASH_INFO_AVAILABLE) == true
        }

        // AlarmManager + PendingIntent
        alarmMgr = getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val offIntent = Intent(this, TorchOffReceiver::class.java)
        alarmPi = PendingIntent.getBroadcast(
            this, 0, offIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        btnStart.setOnClickListener {
            timer?.cancel()
            val mins = etMin.text.toString().toLongOrNull() ?: 0L
            val secs = etSec.text.toString().toLongOrNull() ?: 0L
            val total = mins*60 + secs
            if (total <= 0) {
                Toast.makeText(this, "Укажите время больше нуля", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Включаем фонарик
            cameraManager.setTorchMode(cameraId, true)

            // Запускаем UI-таймер
            timer = object: CountDownTimer(total*1000, 1000) {
                override fun onTick(ms: Long) {
                    val s = ms/1000
                    tvCountdown.text = String.format("%02d:%02d", s/60, s%60)
                }
                override fun onFinish() {
                    tvCountdown.text = "00:00"
                }
            }.apply{ start() }

            // Ставим Alarm на выключение
            val trigger = SystemClock.elapsedRealtime() + total*1000
            alarmMgr.setExactAndAllowWhileIdle(
                AlarmManager.ELAPSED_REALTIME_WAKEUP, trigger, alarmPi
            )
        }

        btnStop.setOnClickListener {
            // Останавливаем всё
            timer?.cancel()
            timer = null
            tvCountdown.text = "00:00"
            alarmMgr.cancel(alarmPi)
            cameraManager.setTorchMode(cameraId, false)
        }

        // Если ещё нет admin-прав — попросим
        val comp = ComponentName(this, MyDeviceAdminReceiver::class.java)
        val dpm = getSystemService(DEVICE_POLICY_SERVICE) as DevicePolicyManager
        if (!dpm.isAdminActive(comp)) {
            startActivity(Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN).apply {
                putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, comp)
                putExtra(DevicePolicyManager.EXTRA_ADD_EXPLANATION,
                    "Нужны права для блокировки экрана.")
            })
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        timer?.cancel()
        alarmMgr.cancel(alarmPi)
        cameraManager.setTorchMode(cameraId, false)
    }
}
