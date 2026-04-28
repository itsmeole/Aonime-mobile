package com.example.aonime

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.google.gson.Gson
import com.google.gson.JsonElement
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class DetailUiState(
    val isLoading: Boolean = false,
    val anime: AnimeApiItem? = null,
    val displayedEpisodes: List<EpisodeApiItem> = emptyList(),
    val episodeRanges: List<String> = emptyList(),
    val errorMessage: String? = null
)

class DetailViewModel(
    private val repository: AnimeRepository = AnimeRepository()
) : ViewModel() {

    private val _uiState = MutableLiveData(DetailUiState(isLoading = true))
    val uiState: LiveData<DetailUiState> = _uiState

    private var allEpisodes: List<EpisodeApiItem> = emptyList()
    private var currentRangeIndex: Int = 0
    private var currentSearchQuery: String = ""
    private val pageSize = 50

    fun loadAnimeDetail(slug: String) {
        _uiState.value = DetailUiState(isLoading = true)

        viewModelScope.launch {
            try {
                val detail = withContext(Dispatchers.IO) { repository.getAnimeDetail(slug) }
                
                val animeItem = AnimeApiItem(
                    id = slug,
                    title = detail.title,
                    poster = detail.poster,
                    description = detail.description,
                    type = detail.type,
                    rating = detail.rating,
                    quality = detail.banner,
                    genres = parseGenres(detail.detailInfo?.genres),
                    release = detail.detailInfo?.dateAired
                )

                allEpisodes = detail.episodes ?: emptyList()
                val ranges = calculateRanges(allEpisodes.size)
                
                val initialEpisodes = if (allEpisodes.isNotEmpty()) {
                    allEpisodes.subList(0, minOf(pageSize, allEpisodes.size))
                } else emptyList()

                _uiState.value = DetailUiState(
                    isLoading = false,
                    anime = animeItem,
                    displayedEpisodes = initialEpisodes,
                    episodeRanges = ranges,
                    errorMessage = null
                )
            } catch (exception: Exception) {
                exception.printStackTrace()
                _uiState.value = DetailUiState(
                    isLoading = false,
                    errorMessage = "Gagal memuat detail anime: ${exception.localizedMessage}"
                )
            }
        }
    }

    private fun parseGenres(json: JsonElement?): String {
        if (json == null || json.isJsonNull) return ""
        return try {
            when {
                json.isJsonArray -> {
                    json.asJsonArray.map { 
                        it.asString.substringAfterLast("/").replaceFirstChar { c -> c.uppercase() } 
                    }.joinToString(", ")
                }
                json.isJsonPrimitive && json.asJsonPrimitive.isString -> {
                    json.asString.substringAfterLast("/").replaceFirstChar { c -> c.uppercase() }
                }
                else -> ""
            }
        } catch (e: Exception) {
            ""
        }
    }

    private fun calculateRanges(total: Int): List<String> {
        if (total == 0) return emptyList()
        val ranges = mutableListOf<String>()
        for (i in 0 until total step pageSize) {
            val end = if (i + pageSize > total) total else i + pageSize
            ranges.add("${i + 1} - $end")
        }
        return ranges
    }

    fun setRange(index: Int) {
        currentRangeIndex = index
        currentSearchQuery = ""
        updateDisplayedEpisodes()
    }

    fun searchEpisode(query: String) {
        currentSearchQuery = query
        updateDisplayedEpisodes()
    }

    private fun updateDisplayedEpisodes() {
        val filtered = if (currentSearchQuery.isBlank()) {
            val start = currentRangeIndex * pageSize
            if (start >= allEpisodes.size) emptyList()
            else {
                val end = minOf(start + pageSize, allEpisodes.size)
                allEpisodes.subList(start, end)
            }
        } else {
            allEpisodes.filter { 
                it.number?.contains(currentSearchQuery, ignoreCase = true) == true ||
                it.title?.contains(currentSearchQuery, ignoreCase = true) == true
            }.take(pageSize)
        }
        _uiState.value = _uiState.value?.copy(displayedEpisodes = filtered)
    }

    class Factory(
        private val repository: AnimeRepository = AnimeRepository()
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return DetailViewModel(repository) as T
        }
    }
}