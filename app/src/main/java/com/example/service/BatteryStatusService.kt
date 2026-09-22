package com.example.service

import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.IBinder
import android.util.Log
import androidx.core.content.ContextCompat

class BatteryStatusService : Service() {

    private val TAG = "BatteryStatusService"
    private var batteryReceiver: BroadcastReceiver? = null

    companion object {
        const val ACTION_BATTERY_UPDATE = "com.example.action.BATTERY_UPDATE"
        const val EXTRA_LEVEL = "level"
        const val EXTRA_IS_CHARGING = "is_charging"
    }

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "BatteryStatusService created. Registering ACTION_BATTERY_CHANGED.")

        batteryReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                if (intent.action == Intent.ACTION_BATTERY_CHANGED) {
                    val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
                    val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
                    val pct = if (level >= 0 && scale > 0) (level * 100 / scale.toFloat()).toInt() else 100

                    val status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
                    val isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
                                     status == BatteryManager.BATTERY_STATUS_FULL

                    Log.d(TAG, "Battery Level: $pct%, Charging: $isCharging")

                    // Broadcast locally to notify AssistantViewModel and trigger voice alerts
                    val updateIntent = Intent(ACTION_BATTERY_UPDATE).apply {
                        setPackage(context.packageName)
                        putExtra(EXTRA_LEVEL, pct)
                        putExtra(EXTRA_IS_CHARGING, isCharging)
                    }
                    sendBroadcast(updateIntent)
                }
            }
        }

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(
                batteryReceiver,
                IntentFilter(Intent.ACTION_BATTERY_CHANGED),
                Context.RECEIVER_NOT_EXPORTED
            )
        } else {
            registerReceiver(
                batteryReceiver,
                IntentFilter(Intent.ACTION_BATTERY_CHANGED)
            )
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "BatteryStatusService destroyed. Unregistering receiver.")
        try {
            batteryReceiver?.let { unregisterReceiver(it) }
        } catch (e: Exception) {
            Log.e(TAG, "Error unregistering battery receiver", e)
        }
    }
}
