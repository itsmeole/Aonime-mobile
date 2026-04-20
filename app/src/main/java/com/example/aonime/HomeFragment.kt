package com.example.aonime

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

/**
 * Home screen fragment.
 * Displays:
 *  - Toolbar with Aonime logo
 *  - Featured banner (hero section)
 *  - "Trending Now" horizontal scroll list
 *  - "Latest Episodes" grid section
 */
class HomeFragment : Fragment() {

    private lateinit var trendingAdapter: AnimeAdapter
    private lateinit var latestAdapter: AnimeAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_home, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupTrendingList(view)
        setupLatestList(view)
        setupFeaturedBanner(view)
    }

    private fun setupFeaturedBanner(view: View) {
        // Watch Now button placeholder action
        view.findViewById<android.widget.Button>(R.id.btn_watch_now).setOnClickListener {
            Toast.makeText(requireContext(), "Watch Now – API coming soon!", Toast.LENGTH_SHORT).show()
        }
    }

    private fun setupTrendingList(view: View) {
        trendingAdapter = AnimeAdapter { anime ->
            Toast.makeText(requireContext(), "Selected: ${anime.title}", Toast.LENGTH_SHORT).show()
        }

        val rvTrending = view.findViewById<RecyclerView>(R.id.rv_trending)
        rvTrending.layoutManager = LinearLayoutManager(
            requireContext(),
            LinearLayoutManager.HORIZONTAL,
            false
        )
        rvTrending.adapter = trendingAdapter
        // Load dummy data – replace with API call when ready
        trendingAdapter.submitList(DummyData.trendingAnime)
    }

    private fun setupLatestList(view: View) {
        latestAdapter = AnimeAdapter(isGrid = true) { anime ->
            Toast.makeText(requireContext(), "Selected: ${anime.title}", Toast.LENGTH_SHORT).show()
        }

        val rvLatest = view.findViewById<RecyclerView>(R.id.rv_latest)
        rvLatest.layoutManager = GridLayoutManager(requireContext(), 2)
        rvLatest.adapter = latestAdapter
        // Load dummy data – replace with API call when ready
        latestAdapter.submitList(DummyData.latestEpisodes)
    }
}
