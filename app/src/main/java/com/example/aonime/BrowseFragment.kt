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
            browseViewModel.setSort(it?.firstOrNull() ?: "latest-updated")
        }
        setupSingleSelectFilter(view.findViewById(R.id.filter_season), "Season", FilterData.seasons) { browseViewModel.setSeason(it) }
        setupSingleSelectFilter(view.findViewById(R.id.filter_year), "Year", FilterData.years) { browseViewModel.setYear(it) }
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
        // term_type[] values: Movie | Music | ONA | OVA | Special | TV
        val types = listOf(
            FilterOption("Movie", "Movie"), FilterOption("TV", "TV"),
            FilterOption("OVA", "OVA"), FilterOption("ONA", "ONA"),
            FilterOption("Special", "Special"), FilterOption("Music", "Music")
        )
        // genre[] values: numeric IDs
        val genres = listOf(
            FilterOption("Action", "1"),
            FilterOption("Adventure", "2"),
            FilterOption("Cars", "538"),
            FilterOption("Comedy", "8"),
            FilterOption("Dementia", "453"),
            FilterOption("Demons", "119"),
            FilterOption("Drama", "62"),
            FilterOption("Ecchi", "214"),
            FilterOption("Fantasy", "3"),
            FilterOption("Game", "180"),
            FilterOption("Harem", "215"),
            FilterOption("Historical", "70"),
            FilterOption("Horror", "222"),
            FilterOption("Isekai", "74"),
            FilterOption("Josei", "404"),
            FilterOption("Kids", "46"),
            FilterOption("Magic", "203"),
            FilterOption("Martial Arts", "114"),
            FilterOption("Mecha", "123"),
            FilterOption("Military", "125"),
            FilterOption("Music", "242"),
            FilterOption("Mystery", "57"),
            FilterOption("Parody", "162"),
            FilterOption("Police", "136"),
            FilterOption("Psychological", "73"),
            FilterOption("Romance", "28"),
            FilterOption("Samurai", "163"),
            FilterOption("School", "14"),
            FilterOption("Sci-Fi", "12"),
            FilterOption("Seinen", "50"),
            FilterOption("Shoujo", "252"),
            FilterOption("Shoujo Ai", "235"),
            FilterOption("Shounen", "15"),
            FilterOption("Shounen Ai", "233"),
            FilterOption("Slice of Life", "35"),
            FilterOption("Space", "124"),
            FilterOption("Sports", "29"),
            FilterOption("Super Power", "16"),
            FilterOption("Supernatural", "9"),
            FilterOption("Thriller", "54"),
            FilterOption("Vampire", "58")
        )
        // status[] values: currently-airing | finished-airing | not-yet-aired
        val statuses = listOf(
            FilterOption("Currently Airing", "currently-airing"),
            FilterOption("Finished Airing", "finished-airing"),
            FilterOption("Not Yet Aired", "not-yet-aired")
        )
        // sort values for /api/filter
        val sorts = listOf(
            FilterOption("Latest Updated", "latest-updated"),
            FilterOption("Score", "score"),
            FilterOption("Name A-Z", "name-az"),
            FilterOption("Release Date", "release-date")
        )
        val seasons = listOf(
            FilterOption("Fall", "fall"), FilterOption("Summer", "summer"),
            FilterOption("Spring", "spring"), FilterOption("Winter", "winter")
        )
        val years = (2026 downTo 2000).map { FilterOption(it.toString(), it.toString()) }
        // rating[] values: G | PG | PG-13 | R | R+ | Rx
        val ratings = listOf(
            FilterOption("G", "G"), FilterOption("PG", "PG"), FilterOption("PG-13", "PG-13"),
            FilterOption("R", "R"), FilterOption("R+", "R+"), FilterOption("Rx", "Rx")
        )
        // language[] values: sub | dub
        val languages = listOf(
            FilterOption("Sub", "sub"),
            FilterOption("Dub", "dub")
        )
    }
}
