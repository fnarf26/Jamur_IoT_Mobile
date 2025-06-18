package com.fendi.jamuriot.model

import com.google.gson.annotations.SerializedName

data class PetugasData(
    val name: String,
    val role: String,
    val email: String,
    val kumbung: List<String> = emptyList()
)

data class ApiResponse(
    val success: Boolean,
    val message: String,
    val uid: String? = null,
    val error: String? = null
)

data class PetugasRequest(
    val email: String,
    val nama: String,
    val role: String = "petugas",
    val kumbung: List<String> = emptyList()
)

data class PetugasUpdateRequest(
    val kumbung: List<String>
)
