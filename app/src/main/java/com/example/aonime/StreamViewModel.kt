package com.example.aonime

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class StreamUiState(
    val isLoading: Boolean = false,
    val watchingText: String? = null,
    val servers: ServerCategories? = null,
    val embedUrl: String? = null,
    val episodes: List<EpisodeApiItem> = emptyList(),
    val errorMessage: String? = null
)

class StreamViewModel(
    private val repository: AnimeRepository = AnimeRepository()
) : ViewModel() {

    private val _uiState = MutableLiveData(StreamUiState())
    val uiState: LiveData<StreamUiState> = _uiState

    fun loadStreamData(token: String, animeSlug: String?) {
        // Preserve current episodes list when loading new server data
        val currentEpisodes = _uiState.value?.episodes ?: emptyList()
        _uiState.value = _uiState.value?.copy(isLoading = true, embedUrl = null)
        
        viewModelScope.launch {
            try {
                // Load Servers
                val serverResponse = withContext(Dispatchers.IO) { repository.getServers(token) }
                
                // Load Episodes only if needed (initial load)
                val episodes = if (animeSlug != null && currentEpisodes.isEmpty()) {
                    withContext(Dispatchers.IO) { repository.getEpisodes(animeSlug).episodes ?: emptyList() }
                } else {
                    currentEpisodes
                }

                _uiState.value = _uiState.value?.copy(
                    isLoading = false,
                    watchingText = serverResponse.watching,
                    servers = serverResponse.servers,
                    episodes = episodes
                )
                
                // Load first server by default
                val firstServer = serverResponse.servers?.sub?.firstOrNull() 
                    ?: serverResponse.servers?.softsub?.firstOrNull()
                    ?: serverResponse.servers?.dub?.firstOrNull()
                
                firstServer?.linkId?.let { loadSource(it) }
            } catch (e: Exception) {
                _uiState.value = _uiState.value?.copy(
                    isLoading = false,
                    errorMessage = "Gagal memuat data: ${e.localizedMessage}"
                )
            }
        }
    }

    fun loadSource(linkId: String) {
        viewModelScope.launch {
            try {
                val response = withContext(Dispatchers.IO) { repository.getSource(linkId) }
                _uiState.value = _uiState.value?.copy(embedUrl = response.embedUrl)
            } catch (e: Exception) {
                _uiState.value = _uiState.value?.copy(
                    errorMessage = "Gagal memuat video: ${e.localizedMessage}"
                )
            }
        }
    }

    class Factory(
        private val repository: AnimeRepository = AnimeRepository()
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return StreamViewModel(repository) as T
        }
    }
}
