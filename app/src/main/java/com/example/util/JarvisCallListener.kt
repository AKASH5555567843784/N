package com.example.util

import android.content.Context
import android.telephony.PhoneStateListener
import android.telephony.TelephonyManager
import android.util.Log

class JarvisCallListener(
    private val context: Context,
    private val onIncomingCallDetected: (String?) -> Unit
) : PhoneStateListener() {

    private val TAG = "JarvisCallListener"

    override fun onCallStateChanged(state: Int, phoneNumber: String?) {
        super.onCallStateChanged(state, phoneNumber)
        Log.d(TAG, "Call State Changed: $state, Number: $phoneNumber")
        if (state == TelephonyManager.CALL_STATE_RINGING) {
            onIncomingCallDetected(phoneNumber)
        }
    }
}
