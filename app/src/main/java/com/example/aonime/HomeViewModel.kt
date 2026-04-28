package com.example.aonime

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.gson.Gson
import com.google.gson.JsonElement
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class HomeUiState(
    val isLoading: Boolean = false,
    val featured: AnimeApiItem? = null,
    val trending: List<Anime> = emptyList(),
    val latest: List<Anime> = emptyList(),
    val errorMessage: String? = null
)

class HomeViewModel(
    private val repository: AnimeRepository = AnimeRepository()
) : ViewModel() {

    private val _uiState = MutableLiveData(HomeUiState(isLoading = true))
    val uiState: LiveData<HomeUiState> = _uiState

    init {
        loadHome()
    }

    fun loadHome() {
        _uiState.value = _uiState.value?.copy(isLoading = true, errorMessage = null)

        viewModelScope.launch {
            val state = withContext(Dispatchers.IO) {
                try {
                    val home = repository.getHome()
                    val featuredItem = parseBannerSafely(home.banner)

                    HomeUiState(
                        isLoading = false,
                        featured = featuredItem,
                        trending = home.topTrending?.now?.take(10)?.map { it.toAnime() } ?: emptyList(),
                        latest = home.latestUpdates?.take(8)?.map { it.toAnime() } ?: emptyList(),
                        errorMessage = null
                    )
                } catch (exception: Exception) {
                    exception.printStackTrace()
                    HomeUiState(
                        isLoading = false,
                        featured = DummyData.trendingAnime.firstOrNull()?.toApiItem(),
                        trending = DummyData.trendingAnime,
                        latest = DummyData.latestEpisodes,
                        errorMessage = "API Error: ${exception.localizedMessage}"
                    )
                }
            }
            _uiState.value = state
        }
    }

    private fun parseBannerSafely(json: JsonElement?): AnimeApiItem? {
        if (json == null || json.isJsonNull) return null
        val gson = Gson()
        return try {
            when {
                json.isJsonArray -> {
                    val type = object : TypeToken<List<AnimeApiItem>>() {}.type
                    val list = gson.fromJson<List<AnimeApiItem>>(json, type)
                    list.firstOrNull()
                }
                json.isJsonObject -> {
                    gson.fromJson(json, AnimeApiItem::class.java)
                }
                else -> null // Jika String atau tipe lain, abaikan
            }
        } catch (e: Exception) {
            null
        }
    }

    class Factory(
        private val repository: AnimeRepository = AnimeRepository()
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return HomeViewModel(repository) as T
        }
    }
}