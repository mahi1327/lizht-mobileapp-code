package com.lizht.app.data.remote


import retrofit2.http.Body
import retrofit2.http.POST

data class UserDto(val _id: String, val name: String, val email: String)
data class AuthResponse(val user: UserDto, val accessToken: String, val refreshToken: String)

interface AuthApi {
    @POST("api/auth/register")
    suspend fun register(@Body body: Map<String, String>): AuthResponse

    @POST("api/auth/login")
    suspend fun login(@Body body: Map<String, String>): AuthResponse

    @POST("api/auth/refresh")
    suspend fun refresh(@Body body: Map<String, String>): Map<String, String> // { accessToken, refreshToken }

    @POST("api/auth/logout")
    suspend fun logout(@Body body: Map<String, String>)
}
