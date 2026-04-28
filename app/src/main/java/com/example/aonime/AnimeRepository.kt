package com.example.aonime

class AnimeRepository(
    private val apiService: AnimeApiService = AnimeApiClient.service
) {

    suspend fun getHome(): HomeResponse {
        return apiService.getHome()
    }

    suspend fun browseAnime(
        page: Int = 1,
        limit: Int = 24,
        sort: String = "trending",
        keyword: String? = null,
        type: List<String>? = null,
        genre: List<String>? = null,
        status: List<String>? = null,
        season: List<String>? = null,
        year: List<String>? = null,
        rating: List<String>? = null,
        country: List<String>? = null,
        language: List<String>? = null
    ): BrowseResponse {
        return apiService.browseAnime(
            page = page,
            limit = limit,
            sort = sort,
            keyword = keyword,
            type = type,
            genre = genre,
            status = status,
            season = season,
            year = year,
            rating = rating,
            country = country,
            language = language
        )
    }
}