package com.example.aonime

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.appcompat.widget.SearchView
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class BrowseFragment : Fragment() {

    private lateinit var browseAdapter: AnimeAdapter
    private lateinit var browseViewModel: BrowseViewModel
    private var searchJob: Job? = null
    private var searchQuery: String = ""

    private enum class FilterMode { NONE, INCLUDE, EXCLUDE }
    private val genreStates = mutableMapOf<FilterOption, FilterMode>()
    private val ratingStates = mutableMapOf<FilterOption, FilterMode>()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_browse, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        browseViewModel = ViewModelProvider(this, BrowseViewModel.Factory())[BrowseViewModel::class.java]
        setupGrid(view)
        setupSearch(view)
        setupAllFilters(view)
        observeBrowseState()
        browseViewModel.loadBrowse(isInitial = true)
    }

    private fun setupGrid(view: View) {
        browseAdapter = AnimeAdapter(isGrid = true) { anime ->
            navigateToDetail(anime.id)
        }
        val rvGrid = view.findViewById<RecyclerView>(R.id.rv_browse_grid)
        val layoutManager = GridLayoutManager(requireContext(), 2)
        rvGrid.layoutManager = layoutManager
        rvGrid.adapter = browseAdapter

        rvGrid.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)
                if (dy > 0) {
                    val visibleItemCount = layoutManager.childCount
                    val totalItemCount = layoutManager.itemCount
                    val pastVisibleItems = layoutManager.findFirstVisibleItemPosition()

                    if ((visibleItemCount + pastVisibleItems) >= totalItemCount - 4) {
                        browseViewModel.loadBrowse(isInitial = false)
                    }
                }
            }
        })
    }

    private fun navigateToDetail(slug: String) {
        val intent = Intent(requireContext(), DetailActivity::class.java).apply {
            putExtra("EXTRA_SLUG", slug)
        }
        startActivity(intent)
    }

    private fun setupSearch(view: View) {
        val searchView = view.findViewById<SearchView>(R.id.search_view)
        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                searchQuery = query?.trim().orEmpty()
                browseViewModel.setQuery(searchQuery)
                browseViewModel.loadBrowse(isInitial = true)
                searchView.clearFocus()
                return true
            }
            override fun onQueryTextChange(newText: String?): Boolean {
                searchQuery = newText?.trim().orEmpty()
                scheduleBrowseLoad()
                return true
            }
        })
    }

    private fun setupAllFilters(view: View) {
        setupMultiStateFilter(view.findViewById(R.id.filter_genre), "Genre", FilterData.genres, genreStates) { 
            browseViewModel.setGenre(it)
        }
        setupMultiStateFilter(view.findViewById(R.id.filter_rating), "Rating", FilterData.ratings, ratingStates) {
            browseViewModel.setRating(it)
        }
        setupSingleSelectFilter(view.findViewById(R.id.filter_type), "Type", FilterData.types) { browseViewModel.setType(it) }
        setupSingleSelectFilter(view.findViewById(R.id.filter_status), "Status", FilterData.statuses) { browseViewModel.setStatus(it) }
        setupSingleSelectFilter(view.findViewById(R.id.filter_sort), "Sort", FilterData.sorts) { 
            browseViewModel.setSort(it?.firstOrNull() ?: "trending") 
        }
        setupSingleSelectFilter(view.findViewById(R.id.filter_season), "Season", FilterData.seasons) { browseViewModel.setSeason(it) }
        setupSingleSelectFilter(view.findViewById(R.id.filter_year), "Year", FilterData.years) { browseViewModel.setYear(it) }
        setupSingleSelectFilter(view.findViewById(R.id.filter_country), "Country", FilterData.countries) { browseViewModel.setCountry(it) }
        setupSingleSelectFilter(view.findViewById(R.id.filter_language), "Lang", FilterData.languages) { browseViewModel.setLanguage(it) }
    }

    private fun setupMultiStateFilter(
        container: View,
        defaultLabel: String,
        options: List<FilterOption>,
        stateMap: MutableMap<FilterOption, FilterMode>,
        onChanged: (List<String>?) -> Unit
    ) {
        val tvLabel = container.findViewById<TextView>(R.id.tv_label)
        updateFilterLabel(tvLabel, defaultLabel, stateMap)
        
        container.setOnClickListener {
            val adapter = object : ArrayAdapter<FilterOption>(requireContext(), android.R.layout.simple_list_item_1, options) {
                override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
                    val view = super.getView(position, convertView, parent)
                    val text = view.findViewById<TextView>(android.R.id.text1)
                    val option = getItem(position)!!
                    val mode = stateMap[option] ?: FilterMode.NONE

                    val (prefix, colorRes) = when (mode) {
                        FilterMode.INCLUDE -> "(+) " to R.color.badge_ona
                        FilterMode.EXCLUDE -> "(-) " to R.color.badge_ova
                        FilterMode.NONE -> "" to R.color.text_primary
                    }

                    text.text = "$prefix${option.label}"
                    text.setTextColor(ContextCompat.getColor(context, colorRes))
                    return view
                }
            }

            val dialog = AlertDialog.Builder(requireContext())
                .setTitle("Select $defaultLabel")
                .setAdapter(adapter, null)
                .setPositiveButton("Apply") { _, _ ->
                    val values = stateMap.entries.filter { it.value != FilterMode.NONE }.map { (opt, mode) ->
                        if (mode == FilterMode.EXCLUDE) "-${opt.value}" else opt.value
                    }
                    onChanged(values.takeIf { it.isNotEmpty() })
                    browseViewModel.loadBrowse(isInitial = true)
                    updateFilterLabel(tvLabel, defaultLabel, stateMap)
                }
                .setNeutralButton("Clear All") { _, _ ->
                    stateMap.clear()
                    updateFilterLabel(tvLabel, defaultLabel, stateMap)
                    onChanged(null)
                    browseViewModel.loadBrowse(isInitial = true)
                }
                .create()

            dialog.show()

            dialog.listView.setOnItemClickListener { _, _, position, _ ->
                val option = options[position]
                val currentMode = stateMap[option] ?: FilterMode.NONE
                stateMap[option] = when (currentMode) {
                    FilterMode.NONE -> FilterMode.INCLUDE
                    FilterMode.INCLUDE -> FilterMode.EXCLUDE
                    FilterMode.EXCLUDE -> FilterMode.NONE
                }
                if (stateMap[option] == FilterMode.NONE) stateMap.remove(option)
                adapter.notifyDataSetChanged()
            }
        }
    }

    private fun updateFilterLabel(tvLabel: TextView, defaultLabel: String, stateMap: Map<FilterOption, FilterMode>) {
        val selected = stateMap.filter { it.value != FilterMode.NONE }
        tvLabel.text = when {
            selected.isEmpty() -> defaultLabel
            selected.size == 1 -> {
                val (opt, mode) = selected.entries.first()
                val prefix = if (mode == FilterMode.EXCLUDE) "(-)" else "(+)"
                "$prefix ${opt.label}"
            }
            else -> "${selected.size} Selected"
        }
    }

    private fun setupSingleSelectFilter(
        container: View,
        defaultLabel: String,
        options: List<FilterOption>,
        onSelected: (List<String>?) -> Unit
    ) {
        val tvLabel = container.findViewById<TextView>(R.id.tv_label)
        tvLabel.text = defaultLabel

        container.setOnClickListener {
            val displayOptions = listOf(FilterOption("All $defaultLabel", "")) + options
            val items = displayOptions.map { it.label }.toTypedArray()

            AlertDialog.Builder(requireContext())
                .setTitle("Select $defaultLabel")
                .setItems(items) { _, which ->
                    val selected = displayOptions[which]
                    tvLabel.text = if (selected.value.isEmpty()) defaultLabel else selected.label
                    onSelected(if (selected.value.isEmpty()) null else listOf(selected.value))
                    browseViewModel.loadBrowse(isInitial = true)
                }
                .show()
        }
    }

    private fun scheduleBrowseLoad() {
        searchJob?.cancel()
        searchJob = viewLifecycleOwner.lifecycleScope.launch {
            delay(500)
            browseViewModel.setQuery(searchQuery)
            browseViewModel.loadBrowse(isInitial = true)
        }
    }

    private fun observeBrowseState() {
        browseViewModel.uiState.observe(viewLifecycleOwner) { state ->
            browseAdapter.submitList(state.items)
            if (!state.isLoading && state.errorMessage != null && state.items.isEmpty()) {
                Toast.makeText(requireContext(), state.errorMessage, Toast.LENGTH_SHORT).show()
            }
        }
    }

    private data class FilterOption(val label: String, val value: String)

    private object FilterData {
        val types = listOf(
            FilterOption("Movie", "movie"), FilterOption("TV", "tv"),
            FilterOption("OVA", "ova"), FilterOption("ONA", "ona"),
            FilterOption("Special", "special"), FilterOption("Music", "music")
        )
        val genres = listOf(
            FilterOption("Action", "47"),
            FilterOption("Adventure", "1"), FilterOption("Avant Garde", "235"),
            FilterOption("Comedy", "7"), FilterOption("Demons", "127"),
            FilterOption("Drama", "66"), FilterOption("Ecchi", "8"),
            FilterOption("Fantasy", "34"), FilterOption("Girls Love", "926"),
            FilterOption("Gourmet", "436"), FilterOption("Harem", "196"),
            FilterOption("Horror", "421"), FilterOption("Isekai", "77"),
            FilterOption("Iyashikei", "225"), FilterOption("Josei", "555"),
            FilterOption("Kids", "35"), FilterOption("Magic", "78"),
            FilterOption("Mahou Shoujo", "857"), FilterOption("Martial Arts", "92"),
            FilterOption("Mecha", "219"), FilterOption("Military", "134"),
            FilterOption("Music", "27"), FilterOption("Mystery", "48"),
            FilterOption("Parody", "356"), FilterOption("Psychological", "240"),
            FilterOption("Reverse Harem", "798"), FilterOption("Romance", "145"),
            FilterOption("School", "9"), FilterOption("Sci-Fi", "36"),
            FilterOption("Seinen", "189"), FilterOption("Shoujo", "183"),
            FilterOption("Shounen", "37"), FilterOption("Slice of Life", "125"),
            FilterOption("Space", "220"), FilterOption("Sports", "10"),
            FilterOption("Super Power", "350"), FilterOption("Supernatural", "49"),
            FilterOption("Suspense", "322"), FilterOption("Thriller", "241"),
            FilterOption("Vampire", "126")
        )
        val statuses = listOf(
            FilterOption("Aired", "info"), FilterOption("Releasing", "releasing"), FilterOption("Completed", "completed")
        )
        val sorts = listOf(
            FilterOption("Trending", "trending"), FilterOption("Latest", "updated_date"),
            FilterOption("Release", "release_date"), FilterOption("Score", "mal_score")
        )
        val seasons = listOf(
            FilterOption("Fall", "fall"), FilterOption("Summer", "summer"),
            FilterOption("Spring", "spring"), FilterOption("Winter", "winter")
        )
        val years = (2026 downTo 2000).map { FilterOption(it.toString(), it.toString()) } + FilterOption("1990s", "1990s")
        val ratings = listOf(
            FilterOption("G", "g"), FilterOption("PG", "pg"), FilterOption("PG-13", "pg_13"),
            FilterOption("R", "r"), FilterOption("R+", "r+"), FilterOption("Rx", "rx")
        )
        val countries = listOf(FilterOption("China", "2"), FilterOption("Japan", "11"))
        val languages = listOf(
            FilterOption("Sub", "sub"), FilterOption("Softsub", "softsub"),
            FilterOption("Dub", "dub"), FilterOption("Sub & Dub", "subdub")
        )
    }
}
