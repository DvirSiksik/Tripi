package com.example.tripi.network

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object RetrofitInstance {
    private const val OPENTRIP_BASE_URL = "https://api.opentripmap.com/0.1/"
    private const val WIKIMEDIA_BASE_URL = "https://commons.wikimedia.org/"

    private val opentripRetrofit = Retrofit.Builder()
        .baseUrl(OPENTRIP_BASE_URL)
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    private val wikimediaRetrofit = Retrofit.Builder()
        .baseUrl(WIKIMEDIA_BASE_URL)
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    val openTripApi: TripApiService = opentripRetrofit.create(TripApiService::class.java)
    val wikimediaApi: TripApiService = wikimediaRetrofit.create(TripApiService::class.java)
}