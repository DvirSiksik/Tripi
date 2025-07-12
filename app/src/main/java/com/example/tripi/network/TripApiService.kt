package com.example.tripi.network

import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query

interface TripApiService {

    @GET("textsearch/json")
    suspend fun getRecommendedTrips(
        @Query("query") query: String = "attractions in Israel",
        @Query("key") apiKey: String = "AIzaSyBJXMokRfxMWkbiJEOpRM7i6ck_Y7Ji7Uk"
    ): Response<GooglePlacesResponse>
}

data class GooglePlacesResponse(
    val results: List<GooglePlaceResult>
)

data class GooglePlaceResult(
    val name: String?,
    val formatted_address: String?,
    val rating: Float?,
    val user_ratings_total: Int?,
    val price_level: Int?,
    val photos: List<GooglePhoto>?,
    val geometry: Geometry?
)

data class GooglePhoto(
    val photo_reference: String
)

data class Geometry(
    val location: Location
)

data class Location(
    val lat: Double,
    val lng: Double
)