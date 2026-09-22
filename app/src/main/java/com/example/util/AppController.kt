package com.example.util

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.util.Log

class AppController(private val context: Context) {

    private val TAG = "AppController"

    /**
     * Data class to represent an installed user application.
     */
    data class AppInfo(
        val label: String,
        val packageName: String
    )

    /**
     * Queries all installed applications that can be launched by the user.
     */
    fun getInstalledApps(): List<AppInfo> {
        val appList = mutableListOf<AppInfo>()
        try {
            val pm = context.packageManager
            val packages = pm.getInstalledApplications(PackageManager.GET_META_DATA)
            for (app in packages) {
                // Filter only apps that have a launch intent
                val launchIntent = pm.getLaunchIntentForPackage(app.packageName)
                if (launchIntent != null) {
                    val label = pm.getApplicationLabel(app).toString()
                    appList.add(AppInfo(label, app.packageName))
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to retrieve installed apps", e)
        }
        return appList
    }

    /**
     * Finds and executes a launch intent for an app matching the spoken text command.
     * Returns true if successfully launched, false otherwise.
     */
    fun launchAppByName(appName: String): Boolean {
        try {
            val pm = context.packageManager
            val apps = getInstalledApps()
            val target = appName.lowercase().trim()

            // Step 1: Direct exact or substring match
            var matchedApp = apps.find { it.label.lowercase() == target }
            if (matchedApp == null) {
                matchedApp = apps.find { it.label.lowercase().contains(target) || target.contains(it.label.lowercase()) }
            }

            if (matchedApp != null) {
                val launchIntent = pm.getLaunchIntentForPackage(matchedApp.packageName)?.apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                if (launchIntent != null) {
                    context.startActivity(launchIntent)
                    Log.d(TAG, "Successfully launched app: ${matchedApp.label}")
                    return true
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error trying to launch application: $appName", e)
        }
        return false
    }
}
