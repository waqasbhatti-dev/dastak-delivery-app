package com.example.data.location

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.io.IOException
import java.util.Locale
import java.util.concurrent.TimeUnit

object RoadRoutingService {
    // Administrator-defined Central Delivery Point in Jhelum City Housing
    const val DEFAULT_CENTRAL_LATITUDE: Double = 32.9957231
    const val DEFAULT_CENTRAL_LONGITUDE: Double = 73.6522227
    const val CENTRAL_LOCALITY: String = "Jhelum City Housing, Jhelum, Pakistan"

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(12, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    data class RouteResult(
        val distanceKm: Double,
        val durationMinutes: Double,
        val originLat: Double,
        val originLng: Double,
        val destLat: Double,
        val destLng: Double
    )

    /**
     * Calculates the real road-route distance using OpenStreetMap OSRM routing service.
     * Note: OSRM expects coordinates in {longitude},{latitude} order.
     */
    suspend fun calculateRoadDistance(
        originLat: Double = DEFAULT_CENTRAL_LATITUDE,
        originLng: Double = DEFAULT_CENTRAL_LONGITUDE,
        destLat: Double,
        destLng: Double
    ): Result<RouteResult> = withContext(Dispatchers.IO) {
        if (destLat == 0.0 && destLng == 0.0) {
            return@withContext Result.failure(IllegalArgumentException("Customer delivery location coordinates are required"))
        }

        val url = String.format(
            Locale.US,
            "https://router.project-osrm.org/route/v1/driving/%.7f,%.7f;%.7f,%.7f?overview=false",
            originLng, originLat, destLng, destLat
        )

        try {
            val request = Request.Builder()
                .url(url)
                .header("User-Agent", "Dastak-Jhelum-Delivery-App/1.0")
                .build()

            val response = httpClient.newCall(request).execute()
            if (!response.isSuccessful) {
                return@withContext Result.failure(
                    IOException("Road routing service returned HTTP ${response.code}. Please check internet and retry.")
                )
            }

            val responseBody = response.body?.string()
                ?: return@withContext Result.failure(IOException("Empty response from road routing service"))

            val json = JSONObject(responseBody)
            val code = json.optString("code")
            if (!code.equals("Ok", ignoreCase = true)) {
                val msg = json.optString("message", "Route calculation could not be completed")
                return@withContext Result.failure(IOException("OSRM Routing Error: $msg"))
            }

            val routes = json.optJSONArray("routes")
            if (routes == null || routes.length() == 0) {
                return@withContext Result.failure(IOException("No road route found between the central warehouse and your delivery location"))
            }

            val primaryRoute = routes.getJSONObject(0)
            val distanceMeters = primaryRoute.optDouble("distance", 0.0)
            val durationSeconds = primaryRoute.optDouble("duration", 0.0)

            if (distanceMeters <= 0.0) {
                return@withContext Result.failure(IOException("Invalid road route distance received"))
            }

            // Convert to kilometers, rounded to 2 decimal places (minimum 0.1 km)
            val distanceKmRaw = distanceMeters / 1000.0
            val distanceKm = Math.max(0.1, Math.round(distanceKmRaw * 100.0) / 100.0)
            val durationMin = Math.round((durationSeconds / 60.0) * 10.0) / 10.0

            Result.success(
                RouteResult(
                    distanceKm = distanceKm,
                    durationMinutes = durationMin,
                    originLat = originLat,
                    originLng = originLng,
                    destLat = destLat,
                    destLng = destLng
                )
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
