package com.fendi.jamuriot.model

import android.util.Log

data class SensorHistoryData(
    val sensor: String,
    val timestamp: String,
    var temperature: Float = 0f,
    var humidity: Float = 0f
) {
    init {
        // More flexible regex patterns
        val tempMatch = Regex("[Ss]uhu\\s*:?\\s*(\\d+(?:\\.\\d+)?)[°]?[Cc]?").find(sensor)
        val humMatch = Regex("[Kk]elembapan\\s*:?\\s*(\\d+(?:\\.\\d+)?)[%]?").find(sensor)

        // Add logging
        Log.d("SensorData", "Parsing: '$sensor'")
        Log.d("SensorData", "Temp match: ${tempMatch?.groupValues}")
        Log.d("SensorData", "Humidity match: ${humMatch?.groupValues}")

        if (tempMatch != null) {
            temperature = tempMatch.groupValues[1].toFloatOrNull() ?: 0f
            Log.d("SensorData", "Parsed temperature: $temperature")
        }

        if (humMatch != null) {
            humidity = humMatch.groupValues[1].toFloatOrNull() ?: 0f
            Log.d("SensorData", "Parsed humidity: $humidity")
        }
    }
}