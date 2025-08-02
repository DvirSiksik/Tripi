package com.example.tripi.network
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query

interface TripApiService {

    @GET("textsearch/json")
    suspend fun getRecommendedTrips(
        @Query("query") query: String = "attractions in Israel",
        @Query("key") apiKey: String = "GOOGLE-APIKEY"
    ): Response<GooglePlacesResponse>
    @GET("textsearch/json")
    suspend fun searchPlaces(
        @Query("query") query: String,
        @Query("key") apiKey: String= "GOOGLE-APIKEY",
        @Query("language") language: String = "he",
        @Query("region") region: String = "il"
    ): Response<GooglePlacesResponse>
}

data class GooglePlacesResponse(
    val status: String?,
    val results: List<GooglePlaceResult>
)

data class GooglePlaceResult(
    val place_id:String?,
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