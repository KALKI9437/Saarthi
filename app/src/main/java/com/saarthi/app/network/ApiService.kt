package com.saarthi.app.network

import retrofit2.Response
import retrofit2.http.*

data class AdviceResponse(
    val status: String,
    val guidance: String,
    val confidence: Double
)

interface ApiService {

    @POST("/api/advice/request")
    suspend fun getAdvice(

        @Header("x-auth-token")
        token: String,

        @Body
        data: Map<String, String>

    ): Response<AdviceResponse>
}