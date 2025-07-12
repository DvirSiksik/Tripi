package com.example.tripi.network

import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface TripApiService {
    @GET("en/places/radius")
    suspend fun getPlacesNearby(
        @Query("radius") radius: Int = 10000,
        @Query("lon") longitude: Double,
        @Query("lat") latitude: Double,
        @Query("kinds") categories: String = "interesting_places",
        @Query("rate") minRating: Int = 3,
        @Query("format") format: String = "json",
        @Query("limit") limit: Int = 20,
        @Query("apikey") apiKey: String = "5ae2e3f221c38a28845f05b601e912edf983ca6d6be5be042d5916f9"
    ): Response<OpenTripMapResponse>

    @GET("en/places/xid/{xid}")
    suspend fun getPlaceDetails(
        @Path("xid") xid: String,
        @Query("apikey") apiKey: String = "5ae2e3f221c38a28845f05b601e912edf983ca6d6be5be042d5916f9"
    ): Response<PlaceDetails>

    @GET("w/api.php")
    suspend fun getWikimediaImage(
        @Query("action") action: String = "query",
        @Query("titles") titles: String,
        @Query("prop") prop: String = "imageinfo",
        @Query("iiprop") iiprop: String = "url",
        @Query("format") format: String = "json"
    ): Response<WikimediaResponse>
}

data class OpenTripMapResponse(
    val features: List<PlaceFeature>
)

data class PlaceFeature(
    val properties: PlaceProperties,
    val geometry: Geometry,
    val xid: String
)

data class PlaceProperties(
    val name: String,
    val kinds: String?,
    val rate: Int?
)

data class Geometry(
    val coordinates: List<Double>
)

data class PlaceDetails(
    val xid: String,
    val name: String,
    val wikipedia_extracts: WikipediaExtracts?,
    val preview: Preview?,
    val wikidata: String?
)

data class WikipediaExtracts(
    val text: String?
)

data class Preview(
    val source: String?
)

data class WikimediaResponse(
    val query: WikimediaQuery?
)

data class WikimediaQuery(
    val pages: Map<String, Page>?
)

data class Page(
    val imageinfo: List<ImageInfo>?
)

data class ImageInfo(
    val url: String?
)