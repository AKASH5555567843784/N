package com.example.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.provider.Settings
import android.util.Log
import android.view.Gravity
import android.view.WindowManager
import androidx.compose.ui.platform.ComposeView
import androidx.core.app.NotificationCompat
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.example.MainActivity
import com.example.ui.CoreState
import com.example.ui.FloatingOverlayView
import kotlinx.coroutines.flow.MutableStateFlow

class AssistantForegroundService : Service() {
    private val TAG = "AssistantService"
    private val CHANNEL_ID = "ninety_five_foreground_channel"
    private val NOTIFICATION_ID = 9595

    private var windowManager: WindowManager? = null
    private var composeView: ComposeView? = null
    private var lifecycleOwner: OverlayLifecycleOwner? = null

    companion object {
        // Shared companion objects to dynamically sync state between AssistantViewModel and Floating UI
        var onIndicatorClickCallback: (() -> Unit)? = null
        val currentCoreState = MutableStateFlow(CoreState.IDLE)
        val currentRmsDb = MutableStateFlow(0f)
    }

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "Foreground service created.")
        createNotificationChannel()
        startForeground(NOTIFICATION_ID, buildNotification("Ninety Five Assistant Active", "Listening offline for 'Hey Ninety Five' or 'Jarvis' in the background."))
        
        // Check overlay permission and setup WindowManager floating indicator
        if (canDrawOverlays()) {
            setupFloatingIndicator()
        } else {
            Log.w(TAG, "Missing SYSTEM_ALERT_WINDOW permission. Floating overlay cannot be created.")
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action
        if (action == "STOP_SERVICE") {
            Log.d(TAG, "Stopping foreground service via action.")
            stopSelf()
            return START_NOT_STICKY
        }
        
        // Re-check overlay permission if service started again
        if (composeView == null && canDrawOverlays()) {
            setupFloatingIndicator()
        }
        
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    private fun canDrawOverlays(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Settings.canDrawOverlays(this)
        } else {
            true
        }
    }

    private fun setupFloatingIndicator() {
        try {
            windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager

            val layoutType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            } else {
                @Suppress("DEPRECATION")
                WindowManager.LayoutParams.TYPE_PHONE
            }

            val params = WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                layoutType,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
                PixelFormat.TRANSLUCENT
            ).apply {
                gravity = Gravity.TOP or Gravity.START
                x = 100
                y = 350
            }

            lifecycleOwner = OverlayLifecycleOwner().apply { start() }

            composeView = ComposeView(this).apply {
                setViewTreeLifecycleOwner(lifecycleOwner)
                setViewTreeViewModelStoreOwner(lifecycleOwner)
                setViewTreeSavedStateRegistryOwner(lifecycleOwner)

                setContent {
                    FloatingOverlayView(
                        coreStateFlow = currentCoreState,
                        rmsDbFlow = currentRmsDb,
                        windowManager = windowManager!!,
                        layoutParams = params,
                        composeView = this,
                        onIndicatorClicked = {
                            onIndicatorClickCallback?.invoke()
                        }
                    )
                }
            }

            windowManager?.addView(composeView, params)
            Log.d(TAG, "Floating AI overlay mounted successfully.")
        } catch (e: Exception) {
            Log.e(TAG, "Error creating floating UI overlay", e)
        }
    }

    private fun removeFloatingIndicator() {
        try {
            composeView?.let {
                windowManager?.removeView(it)
            }
            lifecycleOwner?.destroy()
        } catch (e: Exception) {
            Log.e(TAG, "Error removing floating overlay view", e)
        } finally {
            composeView = null
            lifecycleOwner = null
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Ninety Five Service Channel",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Keeps Ninety Five background engine and wake word active."
            }
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    private fun buildNotification(title: String, text: String): Notification {
        val notificationIntent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            notificationIntent,
            PendingIntent.FLAG_IMMUTABLE
        )

        val stopIntent = Intent(this, AssistantForegroundService::class.java).apply {
            action = "STOP_SERVICE"
        }
        val stopPendingIntent = PendingIntent.getService(
            this,
            1,
            stopIntent,
            PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(text)
            .setSmallIcon(android.R.drawable.ic_btn_speak_now)
            .setContentIntent(pendingIntent)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Deactivate 95", stopPendingIntent)
            .setOngoing(true)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .build()
    }

    override fun onDestroy() {
        super.onDestroy()
        removeFloatingIndicator()
        Log.d(TAG, "Foreground service destroyed.")
    }
}
