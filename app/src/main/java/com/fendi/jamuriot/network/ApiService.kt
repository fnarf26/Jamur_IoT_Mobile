package com.fendi.jamuriot.api

import com.fendi.jamuriot.model.ApiResponse
import com.fendi.jamuriot.model.PetugasRequest
import com.fendi.jamuriot.model.PetugasUpdateRequest
import retrofit2.Call
import retrofit2.http.*

interface ApiService {
    @POST("petugas")
    fun createPetugas(@Body request: PetugasRequest): Call<ApiResponse>

    @PUT("petugas/{email}")
    fun updatePetugas(
        @Path("email") email: String,
        @Body request: PetugasUpdateRequest
    ): Call<ApiResponse>

    @DELETE("petugas/{email}")
    fun deletePetugas(@Path("email") email: String): Call<ApiResponse>
}