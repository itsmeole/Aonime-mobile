package com.example.aonime

import android.annotation.SuppressLint
import android.content.pm.ActivityInfo
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.FrameLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class StreamActivity : AppCompatActivity() {

    private lateinit var viewModel: StreamViewModel
    private lateinit var webView: WebView
    private lateinit var episodeAdapter: EpisodeAdapter
    
    private var customView: View? = null
    private var customViewCallback: WebChromeClient.CustomViewCallback? = null

    private val backPressedCallback: OnBackPressedCallback = object : OnBackPressedCallback(false) {
        override fun handleOnBackPressed() {
            if (customView != null) {
                chromeClient.onHideCustomView()
            }
        }
    }

    private val chromeClient: WebChromeClient = object : WebChromeClient() {
        override fun onShowCustomView(view: View?, callback: CustomViewCallback?) {
            if (customView != null) {
                callback?.onCustomViewHidden()
                return
            }
            customView = view
            customViewCallback = callback
            
            backPressedCallback.isEnabled = true
            
            // Force sensor landscape for fullscreen
            requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
            
            findViewById<FrameLayout>(R.id.fullscreen_container).apply {
                visibility = View.VISIBLE
                addView(view)
            }
            findViewById<View>(R.id.main_content).visibility = View.GONE
            
            // Immersive Mode
            window.decorView.systemUiVisibility = (View.SYSTEM_UI_FLAG_FULLSCREEN
                    or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                    or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                    or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                    or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                    or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN)
        }

        override fun onHideCustomView() {
            if (customView == null) return
            
            backPressedCallback.isEnabled = false
            
            // Restore to unspecified orientation
            requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
            
            findViewById<FrameLayout>(R.id.fullscreen_container).apply {
                visibility = View.GONE
                removeView(customView)
            }
            customView = null
            customViewCallback?.onCustomViewHidden()
            findViewById<View>(R.id.main_content).visibility = View.VISIBLE
            
            // Restore system UI
            window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_VISIBLE
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_stream)

        onBackPressedDispatcher.addCallback(this, backPressedCallback)

        val token = intent.getStringExtra("EXTRA_TOKEN")
        val title = intent.getStringExtra("EXTRA_TITLE")
        val epNumber = intent.getStringExtra("EXTRA_EP_NUMBER")
        val animeSlug = intent.getStringExtra("EXTRA_ANIME_SLUG")

        if (token == null) {
            finish()
            return
        }

        setupToolbar()
        updateStreamTitle(title, epNumber)
        setupWebView()
        setupEpisodeList()
        setupViewModel(token, animeSlug)
    }

    private fun setupToolbar() {
        val toolbar = findViewById<Toolbar>(R.id.toolbar_stream)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = ""
        toolbar.setNavigationOnClickListener { 
            if (customView != null) {
                chromeClient.onHideCustomView()
            } else {
                finish()
            }
        }
    }

    private fun updateStreamTitle(title: String?, epNumber: String? = null) {
        val displayTitle = title ?: (epNumber?.let { "Episode $it" } ?: "Streaming")
        findViewById<TextView>(R.id.tv_stream_title).text = displayTitle
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun setupWebView() {
        webView = findViewById(R.id.webview_player)
        val progressBar = findViewById<View>(R.id.loading_player)

        val settings = webView.settings
        settings.javaScriptEnabled = true
        settings.domStorageEnabled = true
        settings.mediaPlaybackRequiresUserGesture = false
        settings.loadWithOverviewMode = true
        settings.useWideViewPort = true
        settings.displayZoomControls = false
        settings.builtInZoomControls = false
        
        webView.setInitialScale(1)
        
        webView.webViewClient = object : WebViewClient() {
            override fun onPageStarted(view: WebView?, url: String?, favicon: android.graphics.Bitmap?) {
                progressBar.visibility = View.VISIBLE
            }
            override fun onPageFinished(view: WebView?, url: String?) {
                progressBar.visibility = View.GONE
                view?.loadUrl("javascript:(function() { " +
                        "var meta = document.createElement('meta');" +
                        "meta.name = 'viewport';" +
                        "meta.content = 'width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no';" +
                        "document.getElementsByTagName('head')[0].appendChild(meta);" +
                        "})()")
            }
        }
        
        webView.webChromeClient = chromeClient
    }

    private fun setupEpisodeList() {
        episodeAdapter = EpisodeAdapter { episode ->
            updateStreamTitle(episode.title, episode.number)
            episode.token?.let { viewModel.loadStreamData(it, null) }
        }
        val rv = findViewById<RecyclerView>(R.id.rv_stream_episodes)
        rv.layoutManager = LinearLayoutManager(this)
        rv.adapter = episodeAdapter
    }

    private fun setupRangeDropdown(ranges: List<String>) {
        val container = findViewById<View>(R.id.filter_episode_range)
        val tvLabel = container.findViewById<TextView>(R.id.tv_label)
        
        if (ranges.isEmpty()) {
            container.visibility = View.GONE
            return
        }
        
        container.visibility = if (ranges.size > 1) View.VISIBLE else View.GONE
        if (tvLabel.text == "Filter") {
            tvLabel.text = ranges.first()
        }

        container.setOnClickListener {
            AlertDialog.Builder(this)
                .setTitle("Select Episode Range")
                .setItems(ranges.toTypedArray()) { _, which ->
                    tvLabel.text = ranges[which]
                    viewModel.setRange(which)
                }
                .show()
        }
    }

    private fun setupViewModel(token: String, slug: String?) {
        viewModel = ViewModelProvider(this, StreamViewModel.Factory())[StreamViewModel::class.java]

        viewModel.uiState.observe(this) { state ->
            state.watchingText?.let { findViewById<TextView>(R.id.tv_watching_desc).text = it }
            
            state.servers?.let { servers ->
                setupDropdown(findViewById(R.id.dropdown_sub), "SUB", servers.sub)
                setupDropdown(findViewById(R.id.dropdown_softsub), "SOFTSUB", servers.softsub)
                setupDropdown(findViewById(R.id.dropdown_dub), "DUB", servers.dub)
            }

            state.embedUrl?.let { url ->
                if (webView.url != url) {
                    webView.loadUrl(url)
                }
            }

            episodeAdapter.submitList(state.episodes)

            if (state.episodeRanges.isNotEmpty()) {
                setupRangeDropdown(state.episodeRanges)
            }

            state.errorMessage?.let {
                Toast.makeText(this, it, Toast.LENGTH_SHORT).show()
            }
        }

        viewModel.loadStreamData(token, slug)
    }

    private fun setupDropdown(container: View, type: String, servers: List<ServerItem>?) {
        val tvLabel = container.findViewById<TextView>(R.id.tv_label)
        
        if (servers.isNullOrEmpty()) {
            container.visibility = View.GONE
            return
        }
        
        container.visibility = View.VISIBLE
        tvLabel.text = "$type (${servers.size})"

        container.setOnClickListener {
            val names = servers.map { it.name ?: "Server" }.toTypedArray()
            AlertDialog.Builder(this)
                .setTitle("Select $type Server")
                .setItems(names) { _, which ->
                    val selected = servers[which]
                    tvLabel.text = "$type: ${selected.name}"
                    selected.linkId?.let { viewModel.loadSource(it) }
                }
                .show()
        }
    }

    override fun onDestroy() {
        webView.destroy()
        super.onDestroy()
    }
}
