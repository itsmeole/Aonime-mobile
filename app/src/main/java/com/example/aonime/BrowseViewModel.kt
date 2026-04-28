package com.example.aonime

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class BrowseUiState(
    val isLoading: Boolean = false,
    val isPaginationLoading: Boolean = false,
    val items: List<Anime> = emptyList(),
    val errorMessage: String? = null,
    val isLastPage: Boolean = false
)

class BrowseViewModel(
    private val repository: AnimeRepository = AnimeRepository()
) : ViewModel() {

    private val _uiState = MutableLiveData(BrowseUiState(isLoading = true))
    val uiState: LiveData<BrowseUiState> = _uiState

    private var currentPage: Int = 1
    private var currentQuery: String = ""
    private var currentSort: String = "trending"
    private var currentType: List<String>? = null
    private var currentGenre: List<String>? = null
    private var currentStatus: List<String>? = null
    private var currentSeason: List<String>? = null
    private var currentYear: List<String>? = null
    private var currentRating: List<String>? = null
    private var currentCountry: List<String>? = null
    private var currentLanguage: List<String>? = null

    fun setQuery(query: String) { currentQuery = query }
    fun setSort(sort: String) { currentSort = sort }
    fun setType(type: List<String>?) { currentType = type }
    fun setGenre(genre: List<String>?) { currentGenre = genre }
    fun setStatus(status: List<String>?) { currentStatus = status }
    fun setSeason(season: List<String>?) { currentSeason = season }
    fun setYear(year: List<String>?) { currentYear = year }
    fun setRating(rating: List<String>?) { currentRating = rating }
    fun setCountry(country: List<String>?) { currentCountry = country }
    fun setLanguage(language: List<String>?) { currentLanguage = language }

    fun loadBrowse(isInitial: Boolean = true) {
        if (isInitial) {
            currentPage = 1
            _uiState.value = _uiState.value?.copy(isLoading = true, errorMessage = null, items = emptyList(), isLastPage = false)
        } else {
            if (_uiState.value?.isPaginationLoading == true || _uiState.value?.isLastPage == true) return
            _uiState.value = _uiState.value?.copy(isPaginationLoading = true)
        }

        viewModelScope.launch {
            val state = withContext(Dispatchers.IO) {
                try {
                    val results = repository.browseAnime(
                        page = currentPage,
                        limit = 24,
                        sort = if (currentQuery.isBlank()) currentSort else "updated_date",
                        keyword = currentQuery.ifBlank { null },
                        type = currentType,
                        genre = currentGenre,
                        status = currentStatus,
                        season = currentSeason,
                        year = currentYear,
                        rating = currentRating,
                        country = currentCountry,
                        language = currentLanguage
                    )
                    
                    val newItems = results.data?.map { it.toAnime() } ?: emptyList()
                    val currentItems = if (isInitial) emptyList() else _uiState.value?.items ?: emptyList()
                    
                    currentPage++
                    
                    _uiState.value?.copy(
                        isLoading = false,
                        isPaginationLoading = false,
                        items = currentItems + newItems,
                        errorMessage = null,
                        isLastPage = newItems.isEmpty() || newItems.size < 24
                    )
                } catch (exception: Exception) {
                    _uiState.value?.copy(
                        isLoading = false,
                        isPaginationLoading = false,
                        errorMessage = "API Error: ${exception.localizedMessage}",
                        isLastPage = true
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
            return BrowseViewModel(repository) as T
        }
    }
}