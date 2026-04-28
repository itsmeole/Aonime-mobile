package com.example.aonime

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import coil.load

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
    private lateinit var homeViewModel: HomeViewModel

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_home, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        homeViewModel = ViewModelProvider(this, HomeViewModel.Factory())[HomeViewModel::class.java]
        setupTrendingList(view)
        setupLatestList(view)
        setupFeaturedBanner(view)
        observeHomeState(view)
        homeViewModel.loadHome()
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
        trendingAdapter.submitList(emptyList())
    }

    private fun setupLatestList(view: View) {
        latestAdapter = AnimeAdapter(isGrid = true) { anime ->
            Toast.makeText(requireContext(), "Selected: ${anime.title}", Toast.LENGTH_SHORT).show()
        }

        val rvLatest = view.findViewById<RecyclerView>(R.id.rv_latest)
        rvLatest.layoutManager = GridLayoutManager(requireContext(), 2)
        rvLatest.adapter = latestAdapter
        latestAdapter.submitList(emptyList())
    }

    private fun setupFeaturedBanner(view: View) {
        view.findViewById<android.widget.Button>(R.id.btn_watch_now).setOnClickListener {
            Toast.makeText(requireContext(), "Memuat data anime unggulan dari API", Toast.LENGTH_SHORT).show()
        }
    }

    private fun observeHomeState(view: View) {
        homeViewModel.uiState.observe(viewLifecycleOwner) { state ->
            if (state.isLoading && state.trending.isEmpty() && state.latest.isEmpty()) {
                return@observe
            }

            state.featured?.let { bindFeaturedBanner(view, it) } ?: run {
                view.findViewById<android.widget.TextView>(R.id.tv_featured_title).text =
                    state.errorMessage?.let { "Featured tidak tersedia" } ?: "Featured"
                view.findViewById<android.widget.TextView>(R.id.tv_featured_desc).text =
                    state.errorMessage ?: ""
                view.findViewById<android.widget.ImageView>(R.id.iv_featured_poster)
                    .setBackgroundResource(R.drawable.bg_featured_placeholder)
            }

            trendingAdapter.submitList(state.trending)
            latestAdapter.submitList(state.latest)

            state.errorMessage?.let {
                Toast.makeText(requireContext(), it, Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun bindFeaturedBanner(view: View, item: AnimeApiItem) {
        view.findViewById<android.widget.TextView>(R.id.tv_featured_title).text = item.title ?: "Untitled"
        view.findViewById<android.widget.TextView>(R.id.tv_featured_desc).text =
            item.description?.ifBlank { "Tonton anime unggulan langsung dari AnimeKai API." }
                ?: "Tonton anime unggulan langsung dari AnimeKai API."

        view.findViewById<android.widget.ImageView>(R.id.iv_featured_poster).load(item.poster) {
            crossfade(true)
            placeholder(R.drawable.bg_featured_placeholder)
            error(R.drawable.bg_featured_placeholder)
        }
    }
}
