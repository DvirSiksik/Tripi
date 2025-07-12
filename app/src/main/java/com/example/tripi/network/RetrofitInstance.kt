package com.example.tripi.network

import com.google.gson.GsonBuilder
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object RetrofitInstance {
    private const val GOOGLE_PLACES_BASE_URL = "https://maps.googleapis.com/maps/api/place/"

    private val gson = GsonBuilder().setLenient().create()

    val googlePlacesRetrofit: Retrofit = Retrofit.Builder()
        .baseUrl(GOOGLE_PLACES_BASE_URL)
        .addConverterFactory(GsonConverterFactory.create(gson))
        .build()
}