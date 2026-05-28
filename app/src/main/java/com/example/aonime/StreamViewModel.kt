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
    val animeName: String? = null,
    val currentEpNumber: String? = null,
    /** All available sources (sub + dub) for the current episode */
    val sources: List<SourceItem> = emptyList(),
    /** The active m3u8 URL already routed through CF Worker proxy */
    val activeM3u8Url: String? = null,
    /** Sub sources for the server dropdown */
    val subSources: List<SourceItem> = emptyList(),
    /** Dub sources for the server dropdown */
    val dubSources: List<SourceItem> = emptyList(),
    /** All episodes of this anime for the episode list */
    val episodes: List<EpisodeApiItem> = emptyList(),
    /** Subtitle tracks for the active source */
    val activeTracks: List<SubtitleTrack> = emptyList(),
    val episodeRanges: List<String> = emptyList(),
    val errorMessage: String? = null
)

class StreamViewModel(
    private val repository: AnimeRepository = AnimeRepository()
) : ViewModel() {

    private val _uiState = MutableLiveData(StreamUiState())
    val uiState: LiveData<StreamUiState> = _uiState

    private var animeSlug: String = ""
    private var allEpisodes: List<EpisodeApiItem> = emptyList()
    private var currentRangeIndex: Int = 0
    private val pageSize = 50

    /**
     * Load watch data for a specific episode (slug + episode number).
     * Also loads episode list on first call.
     */
    fun loadEpisode(slug: String, epNumber: String, animeName: String?) {
        animeSlug = slug
        val currentEpisodes = _uiState.value?.episodes ?: emptyList()
        _uiState.value = _uiState.value?.copy(
            isLoading = true,
            animeName = animeName,
            currentEpNumber = epNumber,
            activeM3u8Url = null,
            errorMessage = null
        )

        viewModelScope.launch {
            try {
                val watchResp = withContext(Dispatchers.IO) {
                    repository.getWatchData(slug = slug, ep = epNumber)
                }

                val watchData = watchResp.data
                val sources = watchData?.sources ?: emptyList()
                val subSources = sources.filter { it.type == "sub" }
                val dubSources = sources.filter { it.type == "dub" }

                // Load episodes on initial open (when list is empty)
                val episodes = if (allEpisodes.isEmpty()) {
                    val detailResp = withContext(Dispatchers.IO) {
                        repository.getAnimeDetail(slug)
                    }
                    val loaded = detailResp.data?.episodesData?.episodes ?: emptyList()
                    allEpisodes = loaded
                    loaded
                } else {
                    currentEpisodes
                }

                val ranges = calculateRanges(allEpisodes.size)
                val displayedEpisodes = currentDisplayedEpisodes()

                // Default to first sub source, proxied through CF Worker
                val defaultSource = subSources.firstOrNull() ?: dubSources.firstOrNull()
                val proxiedUrl = buildProxiedM3u8(
                    m3u8Url = defaultSource?.m3u8,
                    referer = defaultSource?.referer,
                    apiProxyUrl = defaultSource?.proxyUrl
                )

                _uiState.value = _uiState.value?.copy(
                    isLoading = false,
                    sources = sources,
                    subSources = subSources,
                    dubSources = dubSources,
                    activeM3u8Url = proxiedUrl,
                    activeTracks = defaultSource?.tracks ?: emptyList(),
                    episodes = displayedEpisodes,
                    episodeRanges = ranges
                )
            } catch (e: Exception) {
                e.printStackTrace()
                _uiState.value = _uiState.value?.copy(
                    isLoading = false,
                    errorMessage = "Gagal memuat video: ${e.localizedMessage}"
                )
            }
        }
    }

    /**
     * Switch the active stream to a different source.
     */
    fun selectSource(source: SourceItem) {
        val proxiedUrl = buildProxiedM3u8(
            m3u8Url = source.m3u8,
            referer = source.referer,
            apiProxyUrl = source.proxyUrl
        )
        _uiState.value = _uiState.value?.copy(
            activeM3u8Url = proxiedUrl,
            activeTracks = source.tracks ?: emptyList()
        )
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
        _uiState.value = _uiState.value?.copy(episodes = currentDisplayedEpisodes())
    }

    private fun currentDisplayedEpisodes(): List<EpisodeApiItem> {
        val start = currentRangeIndex * pageSize
        return if (start >= allEpisodes.size) emptyList()
        else {
            val end = minOf(start + pageSize, allEpisodes.size)
            allEpisodes.subList(start, end)
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
