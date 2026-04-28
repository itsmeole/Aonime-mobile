package com.example.aonime

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.EditText
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.google.android.material.appbar.CollapsingToolbarLayout

class DetailActivity : AppCompatActivity() {

    private lateinit var viewModel: DetailViewModel
    private lateinit var episodeAdapter: EpisodeAdapter
    private var currentSlug: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_detail)

        currentSlug = intent.getStringExtra("EXTRA_SLUG")
        val slug = currentSlug ?: run {
            finish()
            return
        }

        setupToolbar()
        setupRecyclerView()
        setupEpisodeSearch()
        setupViewModel(slug)
    }

    private fun setupToolbar() {
        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        toolbar.setNavigationOnClickListener { finish() }
    }

    private fun setupRecyclerView() {
        episodeAdapter = EpisodeAdapter { episode ->
            val intent = Intent(this, StreamActivity::class.java).apply {
                putExtra("EXTRA_TOKEN", episode.token)
                putExtra("EXTRA_TITLE", episode.title)
                putExtra("EXTRA_EP_NUMBER", episode.number)
                putExtra("EXTRA_ANIME_SLUG", currentSlug)
            }
            startActivity(intent)
        }
        val rvEpisodes = findViewById<RecyclerView>(R.id.rv_episodes)
        rvEpisodes.layoutManager = LinearLayoutManager(this)
        rvEpisodes.adapter = episodeAdapter
    }

    private fun setupEpisodeSearch() {
        val etSearch = findViewById<EditText>(R.id.et_search_episode)
        etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                viewModel.searchEpisode(s?.toString().orEmpty())
            }
        })
    }

    private fun setupViewModel(slug: String) {
        viewModel = ViewModelProvider(this, DetailViewModel.Factory())[DetailViewModel::class.java]
        
        viewModel.uiState.observe(this) { state ->
            findViewById<ProgressBar>(R.id.loading_detail).visibility = if (state.isLoading) View.VISIBLE else View.GONE
            
            state.anime?.let { bindAnimeDetail(it) }
            episodeAdapter.submitList(state.displayedEpisodes)
            
            setupRangeDropdown(state.episodeRanges)
            
            state.errorMessage?.let {
                Toast.makeText(this, it, Toast.LENGTH_SHORT).show()
            }
        }
        
        viewModel.loadAnimeDetail(slug)
    }

    private fun setupRangeDropdown(ranges: List<String>) {
        val container = findViewById<View>(R.id.filter_episode_range)
        val tvLabel = container.findViewById<TextView>(R.id.tv_label)
        
        if (ranges.isEmpty()) {
            container.visibility = View.GONE
            return
        }
        
        container.visibility = View.VISIBLE
        if (tvLabel.text == "Filter" || tvLabel.text == "All") {
            tvLabel.text = ranges.first()
        }

        container.setOnClickListener {
            AlertDialog.Builder(this)
                .setTitle("Select Episode Range")
                .setItems(ranges.toTypedArray()) { _, which ->
                    tvLabel.text = ranges[which]
                    viewModel.setRange(which)
                    findViewById<EditText>(R.id.et_search_episode).setText("")
                }
                .show()
        }
    }

    private fun bindAnimeDetail(anime: AnimeApiItem) {
        findViewById<CollapsingToolbarLayout>(R.id.toolbar_layout)
        
        val bannerUrl = if (!anime.quality.isNullOrBlank()) anime.quality else anime.poster
        findViewById<ImageView>(R.id.iv_detail_poster).load(bannerUrl) {
            crossfade(true)
            placeholder(R.drawable.bg_poster_placeholder)
            error(R.drawable.bg_poster_placeholder)
        }
        
        findViewById<TextView>(R.id.tv_detail_title).text = anime.title
        findViewById<TextView>(R.id.tv_detail_type).text = anime.type
        findViewById<TextView>(R.id.tv_detail_year).text = anime.release
        findViewById<TextView>(R.id.tv_detail_genres).text = anime.genres
        findViewById<TextView>(R.id.tv_detail_description).text = anime.description
    }
}