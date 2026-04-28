package com.example.aonime

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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
                    HomeUiState(
                        isLoading = false,
                        featured = home.banner?.firstOrNull(),
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
                        errorMessage = "API Error: ${exception.localizedMessage ?: "API tidak tersedia, menggunakan data cadangan"}"
                    )
                }
            }

            _uiState.value = state
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