package com.rdragon.rd_flashlightautoshutdown

import android.app.admin.DevicePolicyManager
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager

class TorchOffReceiver : BroadcastReceiver() {
    override fun onReceive(ctx: Context, intent: Intent) {
        // Выключаем фонарик
        val camMgr = ctx.getSystemService(Context.CAMERA_SERVICE) as CameraManager
        val camId = camMgr.cameraIdList.first { id ->
            camMgr.getCameraCharacteristics(id)
                .get(CameraCharacteristics.FLASH_INFO_AVAILABLE) == true
        }
        camMgr.setTorchMode(camId, false)

        // Блокируем экран, если админ-права есть
        val dpm = ctx.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
        val comp = ComponentName(ctx, MyDeviceAdminReceiver::class.java)
        if (dpm.isAdminActive(comp)) {
            dpm.lockNow()
        }
    }
}
