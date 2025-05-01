package com.rdragon.rd_flashlightautoshutdown

import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import android.os.CountDownTimer
import android.widget.TextView
import android.content.pm.ActivityInfo

class MainActivity : AppCompatActivity() {
    private lateinit var tvCountdown: TextView
    private lateinit var etMinutes: TextInputEditText
    private lateinit var etSeconds: TextInputEditText
    private lateinit var btnStart: MaterialButton
    private lateinit var btnStop: MaterialButton
    private var countdownTimer: CountDownTimer? = null

    companion object {
        private const val DEVICE_ADMIN_REQUEST_CODE = 1001
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        setContentView(R.layout.activity_main)

        tvCountdown = findViewById(R.id.tvCountdown)
        etMinutes = findViewById(R.id.etMinutes)
        etSeconds = findViewById(R.id.etSeconds)
        btnStart = findViewById(R.id.btnStart)
        btnStop = findViewById(R.id.btnStopService)

        btnStart.setOnClickListener {
            countdownTimer?.cancel()

            val mins = etMinutes.text.toString().toLongOrNull() ?: 0L
            val secs = etSeconds.text.toString().toLongOrNull() ?: 0L
            val totalSeconds = mins * 60 + secs
            if (totalSeconds <= 0) {
                Toast.makeText(this, "Укажите время больше нуля", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Запускаем foreground-сервис
            Intent(this, FlashService::class.java).also { intent ->
                intent.putExtra("duration", totalSeconds)
                ContextCompat.startForegroundService(this, intent)
            }

            // Создаём и запускаем CountDownTimer
            countdownTimer = object : CountDownTimer(totalSeconds * 1000, 1000) {
                override fun onTick(millisUntilFinished: Long) {
                    val s = millisUntilFinished / 1000
                    val display = String.format("%02d:%02d", s / 60, s % 60)
                    tvCountdown.text = display
                }

                override fun onFinish() {
                    tvCountdown.text = "00:00"
                }
            }.apply { start() }
        }

        btnStop.setOnClickListener {
            // Останавливаем сервис
            stopService(Intent(this, FlashService::class.java))
            // Останавливаем таймер и сбрасываем дисплей
            countdownTimer?.cancel()
            countdownTimer = null
            tvCountdown.text = "00:00"
        }

        val compName = ComponentName(this, MyDeviceAdminReceiver::class.java)
        val dpm = getSystemService(DEVICE_POLICY_SERVICE) as DevicePolicyManager
        if (!dpm.isAdminActive(compName)) {
            requestDeviceAdmin()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        // На всякий случай отменяем таймер при выходе из Activity
        countdownTimer?.cancel()
    }

    // Обработчик результата запроса администратора (не обязателен, но можно уведомить)
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 1001 && resultCode != RESULT_OK) {
            Toast.makeText(
                this, "Без прав администратора блокировка экрана не сработает",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    private fun requestDeviceAdmin() {
        val compName = ComponentName(this, MyDeviceAdminReceiver::class.java)
        val intent = Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN).apply {
            putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, compName)
            putExtra(
                DevicePolicyManager.EXTRA_ADD_EXPLANATION,
                "Нужны права администратора для блокировки экрана по истечении таймера."
            )
        }
        ActivityCompat.startActivityForResult(
            this,
            intent,
            DEVICE_ADMIN_REQUEST_CODE,
            null
        )
    }
}