package com.example.darhoney.utils

import android.content.Context
import android.content.pm.PackageManager
import android.location.Address
import android.location.Geocoder
import android.os.Build
import androidx.core.content.ContextCompat
import android.Manifest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import java.util.Locale
import kotlin.coroutines.resume

data class LocationDetails(
    val latitude: Double,
    val longitude: Double,
    val address: String,
    val city: String
)

object MapDefaults {
    val BOGOTA = 4.7110 to -74.0721
    const val DEFAULT_ZOOM = 12f
}

object LocationHelper {

    suspend fun getCurrentLocationDetails(context: Context): LocationDetails? = withContext(Dispatchers.IO) {
        val hasPermission = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        if (!hasPermission) return@withContext null

        val client = LocationServices.getFusedLocationProviderClient(context)
        val location = try {
            client.lastLocation.await()
                ?: client.getCurrentLocation(
                    Priority.PRIORITY_BALANCED_POWER_ACCURACY,
                    CancellationTokenSource().token
                ).await()
        } catch (_: Exception) {
            null
        } ?: return@withContext null

        val place = reverseGeocode(context, location.latitude, location.longitude)
            ?: return@withContext LocationDetails(
                latitude = location.latitude,
                longitude = location.longitude,
                address = "",
                city = ""
            )

        LocationDetails(
            latitude = location.latitude,
            longitude = location.longitude,
            address = place.first,
            city = place.second
        )
    }

    suspend fun reverseGeocode(context: Context, latitude: Double, longitude: Double): Pair<String, String>? =
        withContext(Dispatchers.IO) {
            if (!Geocoder.isPresent()) return@withContext null
            val geocoder = Geocoder(context, Locale.getDefault())
            val address = try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    suspendCancellableCoroutine { continuation ->
                        geocoder.getFromLocation(latitude, longitude, 1, object : Geocoder.GeocodeListener {
                            override fun onGeocode(addresses: MutableList<Address>) {
                                continuation.resume(addresses.firstOrNull())
                            }

                            override fun onError(errorMessage: String?) {
                                continuation.resume(null)
                            }
                        })
                    }
                } else {
                    @Suppress("DEPRECATION")
                    geocoder.getFromLocation(latitude, longitude, 1)?.firstOrNull()
                }
            } catch (_: Exception) {
                null
            } ?: return@withContext null

            val street = listOfNotNull(
                address.thoroughfare,
                address.subThoroughfare
            ).joinToString(" ").trim()

            val city = address.locality
                ?: address.subAdminArea
                ?: address.adminArea
                ?: ""

            (street.ifBlank { address.getAddressLine(0) ?: "" }) to city
        }
}
