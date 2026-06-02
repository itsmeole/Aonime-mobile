package com.example.aonime

import com.google.gson.annotations.SerializedName
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query
import java.util.concurrent.TimeUnit

private val BASE_URL = BuildConfig.BASE_URL

/**
 * Cloudflare Worker proxy URL for HLS streaming.
 * Format: https://<worker-url>/?url=<encoded-m3u8>&referer=<encoded-referer>
 */
val CF_WORKER_PROXY = BuildConfig.CF_WORKER_PROXY

interface AnimeApiService {

    @GET("home")
    suspend fun getHome(): HomeResponse

    @GET("search")
    suspend fun searchAnime(
        @Query("keyword") keyword: String,
        @Query("page") page: Int = 1
    ): LatestResponse

    @GET("filter")
    suspend fun filterAnime(
        @Query("keyword") keyword: String? = null,
        @Query("genre[]") genre: List<String>? = null,
        @Query("season[]") season: List<String>? = null,
        @Query("year[]") year: List<String>? = null,
        @Query("term_type[]") type: List<String>? = null,
        @Query("status[]") status: List<String>? = null,
        @Query("language[]") language: List<String>? = null,
        @Query("rating[]") rating: List<String>? = null,
        @Query("sort") sort: String = "latest-updated",
        @Query("page") page: Int = 1
    ): LatestResponse

    @GET("latest")
    suspend fun getLatest(
        @Query("type") type: String = "latest-updated",
        @Query("page") page: Int = 1
    ): LatestResponse

    @GET("anime/{slug}")
    suspend fun getAnimeDetail(@Path("slug") slug: String): AnimeDetailResponse

    @GET("anime/{slug}/episodes")
    suspend fun getAnimeEpisodes(
        @Path("slug") slug: String,
        @Query("start") start: String? = null,
        @Query("end") end: String? = null
    ): EpisodeListResponse

    @GET("watch/{slug}")
    suspend fun getWatchData(
        @Path("slug") slug: String,
        @Query("ep") ep: String
    ): WatchResponse
}

object AnimeApiClient {
    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .addInterceptor { chain ->
            val request = chain.request().newBuilder()
                .header(
                    "User-Agent",
                    "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36"
                )
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

// ── Response wrappers ──────────────────────────────────────────────────────────

data class HomeResponse(
    @SerializedName("ok") val ok: Boolean = false,
    @SerializedName("data") val data: HomeData? = null
)

data class HomeData(
    @SerializedName("spotlight") val spotlight: List<SpotlightItem>? = emptyList(),
    @SerializedName("latestEpisodes") val latestEpisodes: List<AnimeApiItem>? = emptyList(),
    @SerializedName("newRelease") val newRelease: List<AnimeApiItem>? = emptyList(),
    @SerializedName("topDay") val topDay: List<AnimeApiItem>? = emptyList(),
    @SerializedName("topWeek") val topWeek: List<AnimeApiItem>? = emptyList(),
    @SerializedName("topMonth") val topMonth: List<AnimeApiItem>? = emptyList()
)

data class LatestResponse(
    @SerializedName("ok") val ok: Boolean = false,
    @SerializedName("data") val data: LatestData? = null
)

data class LatestData(
    @SerializedName("results") val results: List<AnimeApiItem>? = emptyList(),
    @SerializedName("currentPage") val currentPage: Int = 1,
    @SerializedName("hasNextPage") val hasNextPage: Boolean = false,
    @SerializedName("keyword") val keyword: String? = null
)

data class AnimeDetailResponse(
    @SerializedName("ok") val ok: Boolean = false,
    @SerializedName("data") val data: AnimeDetailData? = null
)

data class EpisodeListResponse(
    @SerializedName("ok") val ok: Boolean = false,
    @SerializedName("data") val data: EpisodeListData? = null
)

data class WatchResponse(
    @SerializedName("ok") val ok: Boolean = false,
    @SerializedName("data") val data: WatchData? = null
)

// ── Data models ────────────────────────────────────────────────────────────────

data class SpotlightItem(
    @SerializedName("slug") val slug: String?,
    @SerializedName("title") val title: String?,
    @SerializedName("rating") val rating: String?,
    @SerializedName("quality") val quality: String?,
    @SerializedName("hasDub") val hasDub: Boolean = false,
    @SerializedName("hasSub") val hasSub: Boolean = false,
    @SerializedName("date") val date: String?,
    @SerializedName("synopsis") val synopsis: String?,
    @SerializedName("image") val image: String?
)

data class AnimeApiItem(
    @SerializedName("id") val id: String?,
    @SerializedName("slug") val slug: String?,
    @SerializedName("title") val title: String?,
    @SerializedName("image") val image: String?,
    @SerializedName("type") val type: String?,
    @SerializedName("episodes") val episodes: EpisodeCount? = null,
    @SerializedName("date") val date: String?,
    @SerializedName("rank") val rank: Int? = null
)

data class EpisodeCount(
    @SerializedName("sub") val sub: Int? = null,
    @SerializedName("dub") val dub: Int? = null,
    @SerializedName("total") val total: Int? = null
)

data class AnimeDetailData(
    @SerializedName("id") val id: String?,
    @SerializedName("slug") val slug: String?,
    @SerializedName("title") val title: String?,
    @SerializedName("titleJp") val titleJp: String?,
    @SerializedName("image") val image: String?,
    @SerializedName("rating") val rating: String?,
    @SerializedName("quality") val quality: String?,
    @SerializedName("hasDub") val hasDub: Boolean = false,
    @SerializedName("hasSub") val hasSub: Boolean = false,
    @SerializedName("synopsis") val synopsis: String?,
    @SerializedName("type") val type: String?,
    @SerializedName("premiered") val premiered: String?,
    @SerializedName("aired") val aired: String?,
    @SerializedName("status") val status: String?,
    @SerializedName("genres") val genres: List<String>? = emptyList(),
    @SerializedName("malScore") val malScore: Double? = null,
    @SerializedName("duration") val duration: String?,
    @SerializedName("episodeCount") val episodeCount: Int? = null,
    @SerializedName("studios") val studios: List<String>? = emptyList(),
    @SerializedName("episodes") val episodesData: EpisodesWrapper? = null
)

data class EpisodesWrapper(
    @SerializedName("animeId") val animeId: String?,
    @SerializedName("slug") val slug: String?,
    @SerializedName("episodes") val episodes: List<EpisodeApiItem>? = emptyList()
)

data class EpisodeListData(
    @SerializedName("animeId") val animeId: String?,
    @SerializedName("slug") val slug: String?,
    @SerializedName("episodes") val episodes: List<EpisodeApiItem>? = emptyList()
)

data class EpisodeApiItem(
    @SerializedName("number") val number: String?,
    @SerializedName("href") val href: String?,
    @SerializedName("dataIds") val dataIds: String?,
    @SerializedName("hasDub") val hasDub: Boolean = false,
    @SerializedName("hasSub") val hasSub: Boolean = true
)

data class WatchData(
    @SerializedName("episode") val episode: EpisodeApiItem? = null,
    @SerializedName("servers") val servers: List<ServerItem>? = emptyList(),
    @SerializedName("sources") val sources: List<SourceItem>? = emptyList()
)

data class ServerItem(
    @SerializedName("id") val id: String?,
    @SerializedName("name") val name: String?,
    @SerializedName("type") val type: String?   // "sub" | "dub"
)

data class SourceItem(
    @SerializedName("server") val server: String?,
    @SerializedName("type") val type: String?,   // "sub" | "dub"
    @SerializedName("url") val url: String?,
    @SerializedName("m3u8") val m3u8: String?,
    @SerializedName("referer") val referer: String?,
    @SerializedName("proxyUrl") val proxyUrl: String?,
    @SerializedName("tracks") val tracks: List<SubtitleTrack>? = emptyList()
)

data class SubtitleTrack(
    @SerializedName("file") val file: String?,
    @SerializedName("label") val label: String?,
    @SerializedName("kind") val kind: String?,
    @SerializedName("default") val isDefault: Boolean = false,
    @SerializedName("proxyUrl") val proxyUrl: String?
)

// ── Adapter helpers ────────────────────────────────────────────────────────────

fun AnimeApiItem.toAnime(): Anime {
    val epCount = episodes?.sub ?: episodes?.total ?: episodes?.dub ?: 0
    return Anime(
        id = slug ?: id ?: "",
        title = title ?: "Untitled",
        type = (type ?: "TV").ifBlank { "TV" },
        episodes = epCount,
        rating = "",
        posterUrl = image ?: ""
    )
}

fun SpotlightItem.toAnimeApiItemCompat(): AnimeApiItem {
    return AnimeApiItem(
        id = slug,
        slug = slug,
        title = title,
        image = image,
        type = null,
        episodes = null,
        date = date
    )
}

fun SpotlightItem.toAnime(): Anime {
    return Anime(
        id = slug ?: "",
        title = title ?: "Untitled",
        type = "TV",
        episodes = 0,
        rating = rating ?: "",
        posterUrl = image ?: ""
    )
}

/**
 * Build a proxied m3u8 URL.
 *
 * Priority:
 * 1. If the API already returned a proxyUrl (relative path from anikoto-scrap.vercel.app),
 *    use the full absolute URL via the API proxy.
 * 2. Otherwise route through the Cloudflare Worker proxy.
 */
fun buildProxiedM3u8(
    m3u8Url: String?,
    referer: String?,
    apiProxyUrl: String? = null // Ignored now, using CF worker
): String? {
    if (m3u8Url == null) return null

    // Force using Cloudflare Worker proxy instead of Vercel API proxy
    val encoded = java.net.URLEncoder.encode(m3u8Url, "UTF-8")
    var proxy = "$CF_WORKER_PROXY/?url=$encoded"
    if (!referer.isNullOrBlank()) {
        proxy += "&referer=${java.net.URLEncoder.encode(referer, "UTF-8")}"
    }
    return proxy
}