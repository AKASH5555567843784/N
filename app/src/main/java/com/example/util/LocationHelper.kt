package com.example.util

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.util.Log
import androidx.core.content.ContextCompat

class LocationHelper(private val context: Context) {

    private val TAG = "LocationHelper"
    private val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager

    /**
     * Interface/callback to return coordinates (latitude, longitude)
     */
    interface LocationResultListener {
        fun onLocationFound(latitude: Double, longitude: Double)
        fun onError(message: String)
    }

    /**
     * Gets the last known location or starts a single-shot query from GPS/Network providers.
     */
    fun fetchCurrentLocation(listener: LocationResultListener) {
        val finePermission = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION)
        val coarsePermission = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION)

        if (finePermission != PackageManager.PERMISSION_GRANTED && coarsePermission != PackageManager.PERMISSION_GRANTED) {
            listener.onError("Location permissions are not granted.")
            return
        }

        try {
            val isGpsEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
            val isNetworkEnabled = locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)

            if (!isGpsEnabled && !isNetworkEnabled) {
                listener.onError("Both GPS and Network providers are disabled.")
                return
            }

            var bestLocation: Location? = null

            if (isNetworkEnabled) {
                val networkLoc = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
                if (networkLoc != null) {
                    bestLocation = networkLoc
                }
            }

            if (isGpsEnabled) {
                val gpsLoc = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)
                if (gpsLoc != null) {
                    if (bestLocation == null || gpsLoc.accuracy < bestLocation.accuracy) {
                        bestLocation = gpsLoc
                    }
                }
            }

            if (bestLocation != null) {
                Log.d(TAG, "Using last known location: ${bestLocation.latitude}, ${bestLocation.longitude}")
                listener.onLocationFound(bestLocation.latitude, bestLocation.longitude)
                return
            }

            // If no last known location is available, request a single scan
            val provider = if (isNetworkEnabled) LocationManager.NETWORK_PROVIDER else LocationManager.GPS_PROVIDER
            Log.d(TAG, "Requesting single location update from provider: $provider")

            locationManager.requestSingleUpdate(provider, object : LocationListener {
                override fun onLocationChanged(location: Location) {
                    Log.d(TAG, "Location updated: ${location.latitude}, ${location.longitude}")
                    listener.onLocationFound(location.latitude, location.longitude)
                }
                override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}
                override fun onProviderEnabled(provider: String) {}
                override fun onProviderDisabled(provider: String) {}
            }, context.mainLooper)

        } catch (e: Exception) {
            Log.e(TAG, "Exception during location query", e)
            listener.onError("Exception fetching location: ${e.localizedMessage}")
        }
    }
}
