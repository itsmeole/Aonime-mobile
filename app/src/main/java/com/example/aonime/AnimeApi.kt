package com.example.aonime

import com.google.gson.JsonElement
import com.google.gson.annotations.SerializedName
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query
import java.util.concurrent.TimeUnit

private const val BASE_URL = "https://anime-kai-api-main-test.vercel.app/api/"

interface AnimeApiService {

    @GET("home")
    suspend fun getHome(): HomeResponse

    @GET("browse")
    suspend fun browseAnime(
        @Query("page") page: Int = 1,
        @Query("limit") limit: Int = 20,
        @Query("sort") sort: String = "trending",
        @Query("keyword") keyword: String? = null,
        @Query("type[]") type: List<String>? = null,
        @Query("genre[]") genre: List<String>? = null,
        @Query("status[]") status: List<String>? = null,
        @Query("season[]") season: List<String>? = null,
        @Query("year[]") year: List<String>? = null,
        @Query("rating[]") rating: List<String>? = null,
        @Query("country[]") country: List<String>? = null,
        @Query("language[]") language: List<String>? = null
    ): BrowseResponse

    @GET("anime/{slug}")
    suspend fun getAnimeDetail(@Path("slug") slug: String): DetailResponse

    @GET("episodes/{slug}")
    suspend fun getEpisodes(@Path("slug") slug: String): EpisodeResponse
}

object AnimeApiClient {
    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .addInterceptor { chain ->
            val request = chain.request().newBuilder()
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36")
                .build()
            chain.proceed(request)
        }
        .build()

    val service: AnimeApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(AnimeApiService::class.java)
    }
}

data class BrowseResponse(
    @SerializedName("success") val success: Boolean = false,
    @SerializedName("data") val data: List<AnimeApiItem>? = emptyList()
)

data class HomeResponse(
    @SerializedName("success") val success: Boolean = false,
    @SerializedName("banner") val banner: JsonElement? = null,
    @SerializedName("latest_updates") val latestUpdates: List<AnimeApiItem>? = emptyList(),
    @SerializedName("top_trending") val topTrending: TopTrendingResponse? = TopTrendingResponse()
)

data class DetailResponse(
    @SerializedName("success") val success: Boolean = false,
    @SerializedName("title") val title: String? = null,
    @SerializedName("description") val description: String? = null,
    @SerializedName("poster") val poster: String? = null,
    @SerializedName("banner") val banner: String? = null,
    @SerializedName("type") val type: String? = null,
    @SerializedName("rating") val rating: String? = null,
    @SerializedName("mal_score") val malScore: String? = null,
    @SerializedName("detail") val detailInfo: AnimeDetailInfo? = null,
    @SerializedName("episodes") val episodes: List<EpisodeApiItem>? = emptyList()
)

data class AnimeDetailInfo(
    @SerializedName("genres") val genres: JsonElement? = null,
    @SerializedName("date_aired") val dateAired: String? = null
)

data class EpisodeResponse(
    @SerializedName("success") val success: Boolean = false,
    @SerializedName("episodes") val episodes: List<EpisodeApiItem>? = emptyList()
)

data class EpisodeApiItem(
    @SerializedName("number") val number: String?,
    @SerializedName("title") val title: String?,
    @SerializedName("slug") val slug: String?,
    @SerializedName("has_sub") val hasSub: Boolean = false,
    @SerializedName("has_dub") val hasDub: Boolean = false
)

data class TopTrendingResponse(
    @SerializedName("NOW") val now: List<AnimeApiItem>? = emptyList(),
    @SerializedName("DAY") val day: List<AnimeApiItem>? = emptyList(),
    @SerializedName("WEEK") val week: List<AnimeApiItem>? = emptyList(),
    @SerializedName("MONTH") val month: List<AnimeApiItem>? = emptyList()
)

data class AnimeApiItem(
    @SerializedName("id") val id: String?,
    @SerializedName("title") val title: String?,
    @SerializedName("poster") val poster: String? = "",
    @SerializedName("current_episode") val currentEpisode: String? = "",
    @SerializedName("sub_episodes") val subEpisodes: String? = "",
    @SerializedName("dub_episodes") val dubEpisodes: String? = "",
    @SerializedName("type") val type: String? = "TV",
    @SerializedName("rating") val rating: String? = "",
    @SerializedName("quality") val quality: String? = "",
    @SerializedName("description") val description: String? = "",
    @SerializedName("genres") val genres: String? = "",
    @SerializedName("release") val release: String? = ""
)

fun AnimeApiItem.toAnime(): Anime {
    return Anime(
        id = id ?: "",
        title = title ?: "Untitled",
        type = (type ?: "TV").ifBlank { "TV" },
        episodes = episodeCount(),
        rating = (rating ?: quality ?: "—").ifBlank { "—" },
        posterUrl = poster ?: ""
    )
}

fun Anime.toApiItem(): AnimeApiItem {
    return AnimeApiItem(
        id = id,
        title = title,
        poster = posterUrl,
        currentEpisode = episodes.toString(),
        type = type,
        rating = rating,
        description = "Tonton $title secara gratis di Aonime."
    )
}

private fun AnimeApiItem.episodeCount(): Int {
    currentEpisode?.toIntOrNull()?.let { return it }
    subEpisodes?.toIntOrNull()?.let { return it }
    dubEpisodes?.toIntOrNull()?.let { return it }
    return 0
}