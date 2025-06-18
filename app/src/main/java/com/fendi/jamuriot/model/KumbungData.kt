package com.fendi.jamuriot.model

data class KumbungData(
    val imei: String = "",
    val name: String = "",
    val lastUpdate: String = "",
    val temperatureAverage: Double = 0.0,
    val humidityAverage: Double = 0.0
)
