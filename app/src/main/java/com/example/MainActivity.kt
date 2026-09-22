package com.example

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.ContactsContract
import android.telephony.PhoneStateListener
import android.telephony.TelephonyManager
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import com.example.service.BatteryStatusService
import com.example.ui.AssistantViewModel
import com.example.ui.JarvisHUD
import com.example.ui.theme.MyApplicationTheme
import com.example.util.JarvisCallListener
import com.example.util.LocationHelper

class MainActivity : ComponentActivity() {

    private val TAG = "MainActivity"
    private val viewModel: AssistantViewModel by viewModels()

    private var callListener: JarvisCallListener? = null
    private var batteryReceiver: BroadcastReceiver? = null
    private lateinit var locationHelper: LocationHelper

    // Request permissions for speech, camera, telephony, locations and contacts
    private val requestMultiplePermissionsLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val recordAudioGranted = permissions[Manifest.permission.RECORD_AUDIO] ?: false
        val phoneStateGranted = permissions[Manifest.permission.READ_PHONE_STATE] ?: false
        val contactsGranted = permissions[Manifest.permission.READ_CONTACTS] ?: false
        val fineLocationGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] ?: false

        if (recordAudioGranted) {
            Toast.makeText(this, "Speech recognition engine ready.", Toast.LENGTH_SHORT).show()
        }
        if (phoneStateGranted && contactsGranted) {
            registerPhoneStateListener()
        }
        if (fineLocationGranted) {
            fetchRealTimeLocationWeather()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        locationHelper = LocationHelper(this)

        // Launch unified request prompt for all Jarvis-grade features
        requestPermissions()

        // 1. Start the BatteryStatusService
        try {
            val serviceIntent = Intent(this, BatteryStatusService::class.java)
            startService(serviceIntent)
            Log.d(TAG, "BatteryStatusService launched successfully.")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start BatteryStatusService", e)
        }

        // 2. Register Local Receiver to listen for BatteryStatusService updates
        batteryReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                if (intent.action == BatteryStatusService.ACTION_BATTERY_UPDATE) {
                    val pct = intent.getIntExtra(BatteryStatusService.EXTRA_LEVEL, 100)
                    val isCharging = intent.getBooleanExtra(BatteryStatusService.EXTRA_IS_CHARGING, false)
                    viewModel.updateBatteryStatus(pct, isCharging)
                }
            }
        }
        ContextCompat.registerReceiver(
            this,
            batteryReceiver,
            IntentFilter(BatteryStatusService.ACTION_BATTERY_UPDATE),
            ContextCompat.RECEIVER_NOT_EXPORTED
        )

        // 3. Register Telephony Call State Listener
        registerPhoneStateListener()

        // 4. Fetch Weather based on Real-Time Location Services
        fetchRealTimeLocationWeather()

        // 5. Check and request overlay permission (SYSTEM_ALERT_WINDOW) for persistent floating orb
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            if (!android.provider.Settings.canDrawOverlays(this)) {
                val overlayIntent = Intent(
                    android.provider.Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:$packageName")
                )
                try {
                    startActivity(overlayIntent)
                    Toast.makeText(this, "Enable 'Display over other apps' for the Ninety Five floating widget.", Toast.LENGTH_LONG).show()
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to start overlay settings screen", e)
                }
            }
        }

        setContent {
            MyApplicationTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = androidx.compose.ui.graphics.Color(0xFF030712)
                ) {
                    JarvisHUD(viewModel = viewModel, modifier = Modifier.fillMaxSize())
                }
            }
        }
    }

    private fun registerPhoneStateListener() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_PHONE_STATE) == PackageManager.PERMISSION_GRANTED) {
            try {
                val telephonyManager = getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
                callListener = JarvisCallListener(this) { incomingNumber ->
                    val resolvedName = getContactName(this, incomingNumber)
                    viewModel.handleIncomingCall(incomingNumber, resolvedName)
                }
                telephonyManager.listen(callListener, PhoneStateListener.LISTEN_CALL_STATE)
                Log.d(TAG, "PhoneStateListener successfully bound.")
            } catch (e: Exception) {
                Log.e(TAG, "Error registering PhoneStateListener", e)
            }
        }
    }

    private fun fetchRealTimeLocationWeather() {
        locationHelper.fetchCurrentLocation(object : LocationHelper.LocationResultListener {
            override fun onLocationFound(latitude: Double, longitude: Double) {
                Log.d(TAG, "Real GPS Location: $latitude, $longitude")
                viewModel.fetchLiveWeather(latitude, longitude)
            }

            override fun onError(message: String) {
                Log.w(TAG, "Location provider failed: $message. Using New Delhi coordinates as default.")
                viewModel.fetchLiveWeather(28.6139, 77.2090) // New Delhi region fallback
            }
        })
    }

    override fun onDestroy() {
        super.onDestroy()
        try {
            batteryReceiver?.let { unregisterReceiver(it) }
            val telephonyManager = getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
            callListener?.let {
                telephonyManager.listen(it, PhoneStateListener.LISTEN_NONE)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error cleanup in MainActivity", e)
        }
    }

    private fun requestPermissions() {
        val required = arrayOf(
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.READ_PHONE_STATE,
            Manifest.permission.READ_CONTACTS,
            Manifest.permission.READ_CALL_LOG,
            Manifest.permission.CAMERA,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )
        val missing = required.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }
        if (missing.isNotEmpty()) {
            requestMultiplePermissionsLauncher.launch(missing.toTypedArray())
        }
    }

    /**
     * Resolves an incoming phone number into a system Contact Name using ContentProvider queries.
     */
    private fun getContactName(context: Context, phoneNumber: String?): String? {
        if (phoneNumber.isNullOrEmpty()) return null
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CONTACTS) != PackageManager.PERMISSION_GRANTED) {
            return null
        }
        try {
            val uri = Uri.withAppendedPath(
                ContactsContract.PhoneLookup.CONTENT_FILTER_URI,
                Uri.encode(phoneNumber)
            )
            val projection = arrayOf(ContactsContract.PhoneLookup.DISPLAY_NAME)
            context.contentResolver.query(uri, projection, null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val index = cursor.getColumnIndex(ContactsContract.PhoneLookup.DISPLAY_NAME)
                    if (index != -1) {
                        return cursor.getString(index)
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error querying system contacts", e)
        }
        return null
    }
}
