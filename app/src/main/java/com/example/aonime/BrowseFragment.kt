package com.example.aonime

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
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

    private var selectedGenres = mutableSetOf<FilterOption>()
    private var selectedRatings = mutableSetOf<FilterOption>()

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
            Toast.makeText(requireContext(), "Selected: ${anime.title}", Toast.LENGTH_SHORT).show()
        }
        val rvGrid = view.findViewById<RecyclerView>(R.id.rv_browse_grid)
        val layoutManager = GridLayoutManager(requireContext(), 2)
        rvGrid.layoutManager = layoutManager
        rvGrid.adapter = browseAdapter

        // Add scroll listener for pagination
        rvGrid.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)
                if (dy > 0) { // Scrolling down
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
        setupMultiSelectFilter(view.findViewById(R.id.filter_genre), "Genre", FilterData.genres, selectedGenres) { 
            browseViewModel.setGenre(it.map { opt -> opt.value }.takeIf { list -> list.isNotEmpty() })
        }
        setupMultiSelectFilter(view.findViewById(R.id.filter_rating), "Rating", FilterData.ratings, selectedRatings) {
            browseViewModel.setRating(it.map { opt -> opt.value }.takeIf { list -> list.isNotEmpty() })
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

    private fun setupMultiSelectFilter(
        container: View,
        defaultLabel: String,
        options: List<FilterOption>,
        selectedSet: MutableSet<FilterOption>,
        onChanged: (Set<FilterOption>) -> Unit
    ) {
        val tvLabel = container.findViewById<TextView>(R.id.tv_label)
        tvLabel.text = defaultLabel
        
        container.setOnClickListener {
            val items = options.map { it.label }.toTypedArray()
            val checkedItems = options.map { selectedSet.contains(it) }.toBooleanArray()

            AlertDialog.Builder(requireContext())
                .setTitle("Select $defaultLabel")
                .setMultiChoiceItems(items, checkedItems) { _, which, isChecked ->
                    if (isChecked) selectedSet.add(options[which]) else selectedSet.remove(options[which])
                }
                .setPositiveButton("Apply") { _, _ ->
                    tvLabel.text = when {
                        selectedSet.isEmpty() -> defaultLabel
                        selectedSet.size == 1 -> selectedSet.first().label
                        else -> "${selectedSet.size} Selected"
                    }
                    onChanged(selectedSet)
                    browseViewModel.loadBrowse(isInitial = true)
                }
                .setNegativeButton("Clear") { _, _ ->
                    selectedSet.clear()
                    tvLabel.text = defaultLabel
                    onChanged(selectedSet)
                    browseViewModel.loadBrowse(isInitial = true)
                }
                .show()
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
            FilterOption("Action", "47"), FilterOption("Adventure", "1"),
            FilterOption("Comedy", "7"), FilterOption("Drama", "66"),
            FilterOption("Fantasy", "34"), FilterOption("Isekai", "77"),
            FilterOption("Romance", "145"), FilterOption("Shounen", "37"),
            FilterOption("Horror", "421"), FilterOption("Sci-Fi", "36"),
            FilterOption("Slice of Life", "125"), FilterOption("Ecchi", "8")
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
