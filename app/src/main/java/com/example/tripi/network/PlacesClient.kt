package com.example.tripi.network

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object PlacesClient {
    private const val BASE_URL = "https://maps.googleapis.com/maps/api/place/"

    private val retrofit by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    val apiService: TripApiService by lazy {
        retrofit.create(TripApiService::class.java)
    }
}