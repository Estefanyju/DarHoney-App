package com.example.darhoney.utils

import android.content.Context
import android.location.Geocoder
import android.os.Build
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.util.Locale
import kotlin.coroutines.resume

object GeocodingUtils {

    suspend fun resolveCoordinates(
        context: Context,
        name: String,
        address: String,
        city: String
    ): Pair<Double, Double>? = withContext(Dispatchers.IO) {
        if (!Geocoder.isPresent()) return@withContext null

        val queries = buildList {
            if (address.isNotBlank() && city.isNotBlank()) add("$name, $address, $city")
            if (address.isNotBlank() && city.isNotBlank()) add("$address, $city")
            if (name.isNotBlank() && city.isNotBlank()) add("$name, $city")
            if (address.isNotBlank()) add(address)
        }.distinct()

        for (query in queries) {
            geocodeQuery(context, query)?.let { return@withContext it }
        }
        null
    }

    private suspend fun geocodeQuery(context: Context, query: String): Pair<Double, Double>? {
        val geocoder = Geocoder(context, Locale.getDefault())
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                suspendCancellableCoroutine { continuation ->
                    geocoder.getFromLocationName(query, 1, object : Geocoder.GeocodeListener {
                        override fun onGeocode(addresses: MutableList<android.location.Address>) {
                            val address = addresses.firstOrNull()
                            if (address != null) {
                                continuation.resume(address.latitude to address.longitude)
                            } else {
                                continuation.resume(null)
                            }
                        }

                        override fun onError(errorMessage: String?) {
                            continuation.resume(null)
                        }
                    })
                }
            } else {
                @Suppress("DEPRECATION")
                val results = geocoder.getFromLocationName(query, 1)
                results?.firstOrNull()?.let { it.latitude to it.longitude }
            }
        } catch (_: Exception) {
            null
        }
    }
}
