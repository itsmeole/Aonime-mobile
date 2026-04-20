package com.example.aonime

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView

/**
 * Browse screen fragment.
 * Displays:
 *  - Search bar (filters the dummy list)
 *  - Genre filter chips (UI only - no logic wired yet)
 *  - 2-column grid of all anime
 *
 * Backend team: Replace DummyData.allAnime with Retrofit API response.
 * Connect the search EditText to your search endpoint or filter locally.
 */
class BrowseFragment : Fragment() {

    private lateinit var browseAdapter: AnimeAdapter
    private var fullList: List<Anime> = emptyList()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_browse, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupGrid(view)
        setupSearch(view)
        setupGenreChips(view)
    }

    private fun setupGrid(view: View) {
        browseAdapter = AnimeAdapter(isGrid = true) { anime ->
            Toast.makeText(requireContext(), "Selected: ${anime.title}", Toast.LENGTH_SHORT).show()
        }

        val rvGrid = view.findViewById<RecyclerView>(R.id.rv_browse_grid)
        rvGrid.layoutManager = GridLayoutManager(requireContext(), 2)
        rvGrid.adapter = browseAdapter

        // Load full list – replace with API call when ready
        fullList = DummyData.allAnime
        browseAdapter.submitList(fullList)
    }

    private fun setupSearch(view: View) {
        val etSearch = view.findViewById<EditText>(R.id.et_search)
        etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                val query = s?.toString()?.trim()?.lowercase() ?: ""
                val filtered = if (query.isEmpty()) {
                    fullList
                } else {
                    fullList.filter { it.title.lowercase().contains(query) }
                }
                browseAdapter.submitList(filtered)
            }
        })
    }

    private fun setupGenreChips(view: View) {
        // Genre chips are UI-only placeholders for now.
        // Backend team: connect these to genre-filtered API endpoint.
        val chipIds = listOf(
            R.id.chip_all    to "All",
            R.id.chip_action to "Action",
            R.id.chip_fantasy to "Fantasy",
            R.id.chip_romance to "Romance",
            R.id.chip_scifi  to "Sci-Fi",
            R.id.chip_comedy to "Comedy",
        )
        chipIds.forEach { (id, genre) ->
            view.findViewById<com.google.android.material.chip.Chip>(id)?.setOnClickListener {
                Toast.makeText(requireContext(), "Filter: $genre (API coming soon)", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
