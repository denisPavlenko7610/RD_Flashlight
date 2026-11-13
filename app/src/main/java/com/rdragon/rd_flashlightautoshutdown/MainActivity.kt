package com.rdragon.rd_flashlightautoshutdown

import android.app.AlarmManager
import android.app.PendingIntent
import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.os.Build
import android.os.Bundle
import android.os.CountDownTimer
import android.os.SystemClock
import android.provider.Settings
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText

class MainActivity : AppCompatActivity() {
    private lateinit var countdownTextView: TextView
    private lateinit var minutesEditText: TextInputEditText
    private lateinit var startButton: MaterialButton
    private lateinit var stopButton: MaterialButton
    private lateinit var addOneMinuteButton: MaterialButton
    private lateinit var addFiveMinutesButton: MaterialButton

    private var countdownTimer: CountDownTimer? = null
    private var timeLeftMillis: Long = 0

    private lateinit var alarmManager: AlarmManager
    private lateinit var alarmPendingIntent: PendingIntent
    private lateinit var cameraManager: CameraManager
    private var cameraId: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        initializeViews()
        initializeCamera()
        initializeAlarm()
        setupListeners()
        checkDeviceAdmin()
    }

    private fun initializeViews() {
        countdownTextView = findViewById(R.id.tvCountdown)
        minutesEditText = findViewById(R.id.etMinutes)
        startButton = findViewById(R.id.btnStart)
        stopButton = findViewById(R.id.btnStop)
        addOneMinuteButton = findViewById(R.id.btnAdd1Min)
        addFiveMinutesButton = findViewById(R.id.btnAdd5Min)
    }

    private fun initializeCamera() {
        cameraManager = getSystemService(Context.CAMERA_SERVICE) as CameraManager
        try {
            cameraId = cameraManager.cameraIdList.firstOrNull { id ->
                cameraManager.getCameraCharacteristics(id)
                    .get(CameraCharacteristics.FLASH_INFO_AVAILABLE) == true
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun initializeAlarm() {
        alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(this, TorchAutoOffReceiver::class.java)
        alarmPendingIntent = PendingIntent.getBroadcast(
            this, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun setupListeners() {
        startButton.setOnClickListener {
            val currentCameraId = cameraId
            if (currentCameraId == null) {
                Toast.makeText(this, R.string.error_flash_not_found, Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            countdownTimer?.cancel()
            val minutes = minutesEditText.text.toString().toLongOrNull() ?: 0L
            if (minutes <= 0) {
                Toast.makeText(this, R.string.error_invalid_minutes, Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            timeLeftMillis = minutes * 60 * 1000

            try {
                cameraManager.setTorchMode(currentCameraId, true)
            } catch (e: Exception) {
                Toast.makeText(this, R.string.error_flash_turn_on_failed, Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            startCountdownTimer()
            scheduleAlarm()
        }

        stopButton.setOnClickListener {
            stopCountdownTimer()
        }

        addOneMinuteButton.setOnClickListener {
            addMinutes(1)
        }

        addFiveMinutesButton.setOnClickListener {
            addMinutes(5)
        }
    }

    private fun checkDeviceAdmin() {
        val componentName = ComponentName(this, ScreenLockAdminReceiver::class.java)
        val devicePolicyManager = getSystemService(DEVICE_POLICY_SERVICE) as DevicePolicyManager
        if (!devicePolicyManager.isAdminActive(componentName)) {
            startActivity(Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN).apply {
                putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, componentName)
                putExtra(DevicePolicyManager.EXTRA_ADD_EXPLANATION, getString(R.string.device_admin_explanation))
            })
        }
    }

    private fun startCountdownTimer() {
        countdownTimer = object : CountDownTimer(timeLeftMillis, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                timeLeftMillis = millisUntilFinished
                val seconds = millisUntilFinished / 1000
                countdownTextView.text = String.format("%02d:%02d", seconds / 60, seconds % 60)
            }

            override fun onFinish() {
                countdownTextView.text = "00:00"
            }
        }.apply { start() }
    }

    private fun stopCountdownTimer() {
        countdownTimer?.cancel()
        countdownTimer = null
        countdownTextView.text = "00:00"
        timeLeftMillis = 0
        alarmManager.cancel(alarmPendingIntent)

        cameraId?.let { id ->
            try {
                cameraManager.setTorchMode(id, false)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun addMinutes(minutes: Long) {
        val additionalMillis = minutes * 60 * 1000
        if (countdownTimer == null) {
            val currentMinutes = minutesEditText.text.toString().toLongOrNull() ?: 0
            minutesEditText.setText((currentMinutes + minutes).toString())
        } else {
            countdownTimer?.cancel()
            timeLeftMillis += additionalMillis
            startCountdownTimer()
            scheduleAlarm()
        }
    }

    private fun scheduleAlarm() {
        val triggerTime = SystemClock.elapsedRealtime() + timeLeftMillis
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (!alarmManager.canScheduleExactAlarms()) {
                Toast.makeText(this, R.string.error_exact_alarm_permission, Toast.LENGTH_LONG).show()
                try {
                    startActivity(Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM))
                } catch (e: Exception) {
                    e.printStackTrace()
                }
                return
            }
        }
        alarmManager.setExactAndAllowWhileIdle(
            AlarmManager.ELAPSED_REALTIME_WAKEUP, triggerTime, alarmPendingIntent
        )
    }

    override fun onDestroy() {
        super.onDestroy()
        countdownTimer?.cancel()
        alarmManager.cancel(alarmPendingIntent)
        cameraId?.let { id ->
            try {
                cameraManager.setTorchMode(id, false)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}
