package com.example.tripi.network

import retrofit2.http.GET
import retrofit2.http.Query
import com.google.gson.JsonElement


data class GeoapifyResponse(
    val features: List<GeoapifyPlace>
)


data class GeoapifyPlace(
    val properties: GeoapifyProperties,
    val geometry: Geometry
)

data class GeoapifyProperties(
    val name: String?,
    val formatted: String?,
    val details: JsonElement?,
    val datasource: GeoapifyDatasource?
)


data class GeoapifyDetails(
    val image: String?
)


data class GeoapifyDatasource(
    val raw: GeoapifyRaw?
)

data class GeoapifyRaw(
    val image: String?
)


data class Geometry(
    val coordinates: List<Double> // [lon, lat]
)


interface TripApiService {

    @GET("v2/places")
    suspend fun getPlacesNear(
        @Query("categories") categories: String = "tourism.sights",
        @Query("limit") limit: Int = 10,
        @Query("filter") filter: String, // "circle:lon,lat,radius"
        @Query("bias") bias: String,     // "proximity:lon,lat"
        @Query("lang") lang: String = "en",
        @Query("type") type: String = "poi",
        @Query("apiKey") apiKey: String = "28c4b49a37b04f678088c43529183c87"
    ): GeoapifyResponse
}