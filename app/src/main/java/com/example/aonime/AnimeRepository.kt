package com.example.aonime

class AnimeRepository(
    private val apiService: AnimeApiService = AnimeApiClient.service
) {

    suspend fun getHome(): HomeResponse {
        return apiService.getHome()
    }

    /**
     * Search by keyword — maps to /api/search
     */
    suspend fun searchAnime(keyword: String, page: Int = 1): LatestResponse {
        return apiService.searchAnime(keyword = keyword, page = page)
    }

    /**
     * Advanced filter — maps to /api/filter
     */
    suspend fun filterAnime(
        keyword: String? = null,
        genre: List<String>? = null,
        season: List<String>? = null,
        year: List<String>? = null,
        type: List<String>? = null,
        status: List<String>? = null,
        language: List<String>? = null,
        rating: List<String>? = null,
        sort: String = "latest-updated",
        page: Int = 1
    ): LatestResponse {
        return apiService.filterAnime(
            keyword = keyword,
            genre = genre,
            season = season,
            year = year,
            type = type,
            status = status,
            language = language,
            rating = rating,
            sort = sort,
            page = page
        )
    }

    /**
     * Latest listing — maps to /api/latest
     * type: latest-updated | new-release | most-viewed
     */
    suspend fun getLatest(type: String = "latest-updated", page: Int = 1): LatestResponse {
        return apiService.getLatest(type = type, page = page)
    }

    suspend fun getAnimeDetail(slug: String): AnimeDetailResponse {
        return apiService.getAnimeDetail(slug)
    }

    suspend fun getAnimeEpisodes(
        slug: String,
        start: String? = null,
        end: String? = null
    ): EpisodeListResponse {
        return apiService.getAnimeEpisodes(slug = slug, start = start, end = end)
    }

    /**
     * Fetch streaming sources (m3u8 URLs) for a specific episode.
     * Maps to /api/watch/{slug}?ep={number}
     */
    suspend fun getWatchData(slug: String, ep: String): WatchResponse {
        return apiService.getWatchData(slug = slug, ep = ep)
    }
}