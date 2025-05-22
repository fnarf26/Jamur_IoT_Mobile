package com.fendi.jamuriot.model

data class PetugasData(
    val name: String,
    val role: String,
    val email: String,
    val kumbung: List<String> = emptyList()
)
