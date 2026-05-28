package com.example.aonime

import android.annotation.SuppressLint
import android.content.pm.ActivityInfo
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.ImageButton
import android.widget.SeekBar
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import android.widget.AdapterView
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.common.TrackSelectionParameters
import androidx.media3.common.Tracks
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.CaptionStyleCompat
import androidx.media3.ui.PlayerView
import androidx.media3.ui.SubtitleView
import androidx.lifecycle.lifecycleScope
import com.google.android.material.bottomsheet.BottomSheetDialog
import kotlinx.coroutines.launch

class StreamActivity : AppCompatActivity() {

    private lateinit var viewModel: StreamViewModel
    private lateinit var episodeAdapter: EpisodeAdapter

    private var animeSlug: String? = null
    private var currentEpNumber: String? = null

    // ── Active server state ───────────────────────────────────────────────────
    private var activeCategory: String = "sub"   // "sub" | "dub"
    private var subServerIdx: Int = 0
    private var dubServerIdx: Int = 0

    // ── ExoPlayer State ───────────────────────────────────────────────────────
    private var player: ExoPlayer? = null
    private lateinit var playerView: PlayerView
    private var currentM3u8Url: String? = null
    private var currentTracks: List<SubtitleTrack> = emptyList()

    // ── Subtitle Config State ─────────────────────────────────────────────────
    private var subSizeScale = 1.0f
    private var subDelaySec = 0f
    private var subColor = Color.WHITE
    private var isEdgeOutline = true
    private var isEdgeShadow = false
    private var subBgColor = Color.TRANSPARENT
    private var selectedSubIdx = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_stream)

        animeSlug = intent.getStringExtra("EXTRA_ANIME_SLUG")
        currentEpNumber = intent.getStringExtra("EXTRA_EP_NUMBER")
        val animeName = intent.getStringExtra("EXTRA_ANIME_NAME")

        if (animeSlug == null || currentEpNumber == null) {
            Toast.makeText(this, "Data episode tidak valid", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        setupToolbar()
        updateStreamTitle(animeName)
        setupPlayer()
        setupEpisodeList()
        setupViewModel()
    }

    // ── UI Setup ──────────────────────────────────────────────────────────────

    private fun setupToolbar() {
        val toolbar = findViewById<Toolbar>(R.id.toolbar_stream)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = ""
        toolbar.setNavigationOnClickListener { finish() }
    }

    private fun updateStreamTitle(title: String?) {
        val ep = currentEpNumber
        val display = if (title != null && ep != null) "$title · Ep $ep" else title ?: "Streaming"
        findViewById<TextView>(R.id.tv_stream_title).text = display
    }

    private fun setupEpisodeList() {
        episodeAdapter = EpisodeAdapter { episode ->
            currentEpNumber = episode.number
            updateStreamTitle(viewModel.uiState.value?.animeName)
            viewModel.loadEpisode(
                slug = animeSlug!!,
                epNumber = episode.number ?: "1",
                animeName = viewModel.uiState.value?.animeName
            )
        }
        val rv = findViewById<RecyclerView>(R.id.rv_stream_episodes)
        rv.layoutManager = LinearLayoutManager(this)
        rv.adapter = episodeAdapter
    }

    private fun setupRangeDropdown(ranges: List<String>) {
        val container = findViewById<View>(R.id.filter_episode_range)
        val tvLabel = container.findViewById<TextView>(R.id.tv_label)
        if (ranges.isEmpty()) { container.visibility = View.GONE; return }
        container.visibility = if (ranges.size > 1) View.VISIBLE else View.GONE
        if (tvLabel.text == "Filter") tvLabel.text = ranges.first()
        container.setOnClickListener {
            AlertDialog.Builder(this)
                .setTitle("Select Episode Range")
                .setItems(ranges.toTypedArray()) { _, which ->
                    tvLabel.text = ranges[which]
                    viewModel.setRange(which)
                }.show()
        }
    }

    // ── Player Setup ──────────────────────────────────────────────────────────

    private fun setupPlayer() {
        playerView = findViewById(R.id.player_view)

        // Use a standard browser User-Agent to bypass streaming server blocks
        val userAgent = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36"
        val httpDataSourceFactory = androidx.media3.datasource.DefaultHttpDataSource.Factory()
            .setUserAgent(userAgent)
            .setAllowCrossProtocolRedirects(true)
            
        // Use DefaultDataSource so it supports both HTTP and local file:// (for subtitles)
        val dataSourceFactory = androidx.media3.datasource.DefaultDataSource.Factory(this, httpDataSourceFactory)

        val mediaSourceFactory = androidx.media3.exoplayer.source.DefaultMediaSourceFactory(dataSourceFactory)

        player = ExoPlayer.Builder(this)
            .setMediaSourceFactory(mediaSourceFactory)
            .build()
            
        playerView.player = player
        player?.playWhenReady = true
        
        // Enable fullscreen button
        playerView.setFullscreenButtonClickListener { isFullScreen ->
            val videoContainer = playerView.parent as android.widget.FrameLayout
            val toolbar = findViewById<View>(R.id.toolbar_stream)
            val scrollView = findViewById<View>(R.id.scroll_content)
            
            if (isFullScreen) {
                requestedOrientation = android.content.pm.ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
                window.decorView.systemUiVisibility = (View.SYSTEM_UI_FLAG_FULLSCREEN
                        or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                        or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION)
                
                toolbar.visibility = View.GONE
                scrollView.visibility = View.GONE
                videoContainer.layoutParams = android.widget.LinearLayout.LayoutParams(
                    android.widget.LinearLayout.LayoutParams.MATCH_PARENT,
                    android.widget.LinearLayout.LayoutParams.MATCH_PARENT
                )
            } else {
                requestedOrientation = android.content.pm.ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
                window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_VISIBLE
                
                toolbar.visibility = View.VISIBLE
                scrollView.visibility = View.VISIBLE
                
                val heightPx = android.util.TypedValue.applyDimension(
                    android.util.TypedValue.COMPLEX_UNIT_DIP, 220f, resources.displayMetrics
                ).toInt()
                videoContainer.layoutParams = android.widget.LinearLayout.LayoutParams(
                    android.widget.LinearLayout.LayoutParams.MATCH_PARENT,
                    heightPx
                )
            }
        }

        player?.addListener(object : androidx.media3.common.Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                if (playbackState == androidx.media3.common.Player.STATE_BUFFERING) {
                    findViewById<View>(R.id.loading_player).visibility = View.VISIBLE
                } else if (playbackState == androidx.media3.common.Player.STATE_READY) {
                    findViewById<View>(R.id.loading_player).visibility = View.GONE
                }
            }

            override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
                findViewById<View>(R.id.loading_player).visibility = View.GONE
                Toast.makeText(this@StreamActivity, "Player Error: ${error.message}", Toast.LENGTH_LONG).show()
                error.printStackTrace()
            }
        })

        findViewById<ImageButton>(R.id.btn_player_settings).setOnClickListener {
            showSettingsBottomSheet()
        }
        applyCaptionStyle()
    }

    private fun loadOrUpdatePlayer(m3u8Url: String, tracks: List<SubtitleTrack>) {
        if (m3u8Url == currentM3u8Url) return
        currentM3u8Url = m3u8Url
        currentTracks = tracks

        // Select the default subtitle or the first one if available
        selectedSubIdx = tracks.indexOfFirst { it.isDefault }.takeIf { it >= 0 } ?: if (tracks.isNotEmpty()) 0 else -1
        subDelaySec = 0f // Reset delay on new episode

        applyMediaItem(resetPosition = true)
    }

    private fun applyMediaItem(resetPosition: Boolean = false) {
        val m3u8Url = currentM3u8Url ?: return
        findViewById<View>(R.id.loading_player).visibility = View.VISIBLE

        val currentPosition = if (resetPosition) 0L else (player?.currentPosition ?: 0L)
        val currentPlayWhenReady = player?.playWhenReady ?: true

        if (selectedSubIdx >= 0 && selectedSubIdx < currentTracks.size) {
            val track = currentTracks[selectedSubIdx]
            val subUrl = track.proxyUrl?.let { if (it.startsWith("http")) it else "https://anikoto-scrap.vercel.app$it" } ?: track.file ?: ""

            lifecycleScope.launch {
                // Process subtitle delay
                val finalVttUrl = SubtitleProcessor.processAndShiftSubtitle(this@StreamActivity, subUrl, subDelaySec)
                
                val subtitleConfig = finalVttUrl?.let {
                    MediaItem.SubtitleConfiguration.Builder(Uri.parse(it))
                        .setMimeType(MimeTypes.TEXT_VTT)
                        .setLanguage("en")
                        .setSelectionFlags(androidx.media3.common.C.SELECTION_FLAG_DEFAULT)
                        .build()
                }

                val mediaItemBuilder = MediaItem.Builder()
                    .setUri(m3u8Url)
                    .setMimeType(MimeTypes.APPLICATION_M3U8)
                    
                if (subtitleConfig != null) {
                    mediaItemBuilder.setSubtitleConfigurations(listOf(subtitleConfig))
                }
                
                player?.setMediaItem(mediaItemBuilder.build())
                player?.seekTo(currentPosition)
                player?.playWhenReady = currentPlayWhenReady
                player?.prepare()
            }
        } else {
            // No subtitles
            val mediaItem = MediaItem.Builder()
                .setUri(m3u8Url)
                .setMimeType(MimeTypes.APPLICATION_M3U8)
                .build()
                
            player?.setMediaItem(mediaItem)
            player?.seekTo(currentPosition)
            player?.playWhenReady = currentPlayWhenReady
            player?.prepare()
        }
    }

    private fun applyCaptionStyle() {
        val edgeType = if (isEdgeOutline && isEdgeShadow) {
            CaptionStyleCompat.EDGE_TYPE_RAISED
        } else if (isEdgeOutline) {
            CaptionStyleCompat.EDGE_TYPE_OUTLINE
        } else if (isEdgeShadow) {
            CaptionStyleCompat.EDGE_TYPE_DROP_SHADOW
        } else {
            CaptionStyleCompat.EDGE_TYPE_NONE
        }

        val style = CaptionStyleCompat(
            subColor,
            subBgColor,
            Color.TRANSPARENT,
            edgeType,
            Color.BLACK, // Edge color
            null
        )
        playerView.subtitleView?.setStyle(style)
        playerView.subtitleView?.setFractionalTextSize(SubtitleView.DEFAULT_TEXT_SIZE_FRACTION * subSizeScale)
        // Make subtitle not too close to bottom
        playerView.subtitleView?.setBottomPaddingFraction(0.08f)
        val paddingBottomPx = android.util.TypedValue.applyDimension(
            android.util.TypedValue.COMPLEX_UNIT_DIP, 12f, resources.displayMetrics
        ).toInt()
        playerView.subtitleView?.setPadding(0, 0, 0, paddingBottomPx)
    }

    // ── Settings Bottom Sheet ─────────────────────────────────────────────────

    private fun showSettingsBottomSheet() {
        val dialog = BottomSheetDialog(this)
        val view = layoutInflater.inflate(R.layout.bottom_sheet_player_settings, null)
        dialog.setContentView(view)

        // Subtitle Track
        val spinnerTrack = view.findViewById<Spinner>(R.id.spinner_subtitle_track)
        val trackNames = mutableListOf("Off")
        trackNames.addAll(currentTracks.map { it.label ?: "Subtitle" })
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, trackNames)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerTrack.adapter = adapter
        spinnerTrack.setSelection(selectedSubIdx + 1)
        
        spinnerTrack.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(p0: AdapterView<*>?, p1: View?, position: Int, p3: Long) {
                val newIdx = position - 1
                if (newIdx != selectedSubIdx) {
                    selectedSubIdx = newIdx
                    applyMediaItem()
                }
            }
            override fun onNothingSelected(p0: AdapterView<*>?) {}
        }

        // Delay Settings
        val tvDelayValue = view.findViewById<TextView>(R.id.tv_delay_value)
        val seekbarDelay = view.findViewById<SeekBar>(R.id.seekbar_delay)
        
        fun updateDelayUI() {
            tvDelayValue.text = if (subDelaySec == 0f) "Synced" else String.format("%+.1fs", subDelaySec)
            seekbarDelay.progress = ((subDelaySec + 5f) * 10).toInt()
        }
        updateDelayUI()

        seekbarDelay.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) {
                    subDelaySec = (progress - 50) / 10f
                    updateDelayUI()
                }
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) { applyMediaItem() }
        })

        fun adjustDelay(diff: Float, reset: Boolean = false) {
            if (reset) {
                subDelaySec = 0f
            } else {
                subDelaySec += diff
                if (subDelaySec < -5f) subDelaySec = -5f
                if (subDelaySec > 5f) subDelaySec = 5f
            }
            // Round to 1 decimal
            subDelaySec = Math.round(subDelaySec * 10f) / 10f
            updateDelayUI()
            applyMediaItem()
        }

        view.findViewById<Button>(R.id.btn_sync_minus_01).setOnClickListener { adjustDelay(-0.1f) }
        view.findViewById<Button>(R.id.btn_sync_plus_01).setOnClickListener { adjustDelay(0.1f) }
        view.findViewById<Button>(R.id.btn_sync_minus_1).setOnClickListener { adjustDelay(-1f) }
        view.findViewById<Button>(R.id.btn_sync_minus_05).setOnClickListener { adjustDelay(-0.5f) }
        view.findViewById<Button>(R.id.btn_sync_plus_05).setOnClickListener { adjustDelay(0.5f) }
        view.findViewById<Button>(R.id.btn_sync_plus_1).setOnClickListener { adjustDelay(1f) }
        view.findViewById<Button>(R.id.btn_sync_reset).setOnClickListener { adjustDelay(0f, true) }

        // Subtitle Size
        val seekbarSize = view.findViewById<SeekBar>(R.id.seekbar_size)
        val tvSizeValue = view.findViewById<TextView>(R.id.tv_size_value)
        val btnSize75 = view.findViewById<Button>(R.id.btn_size_75)
        val btnSize100 = view.findViewById<Button>(R.id.btn_size_100)
        val btnSize125 = view.findViewById<Button>(R.id.btn_size_125)
        val btnSize150 = view.findViewById<Button>(R.id.btn_size_150)

        fun updateSizeUI() {
            val percentage = (subSizeScale * 100).toInt()
            tvSizeValue.text = "${percentage}%"
            seekbarSize.progress = percentage - 50 // map 50-200 to 0-150. Wait, max is 100 in XML, let's map 50-150 to 0-100
            
            // update buttons
            val dark = R.drawable.bg_btn_dark
            val purple = R.drawable.bg_btn_purple
            val whiteText = Color.WHITE
            val grayText = Color.parseColor("#888899")
            
            btnSize75.setBackgroundResource(if (percentage == 75) purple else dark)
            btnSize75.setTextColor(if (percentage == 75) whiteText else grayText)
            
            btnSize100.setBackgroundResource(if (percentage == 100) purple else dark)
            btnSize100.setTextColor(if (percentage == 100) whiteText else grayText)
            
            btnSize125.setBackgroundResource(if (percentage == 125) purple else dark)
            btnSize125.setTextColor(if (percentage == 125) whiteText else grayText)
            
            btnSize150.setBackgroundResource(if (percentage == 150) purple else dark)
            btnSize150.setTextColor(if (percentage == 150) whiteText else grayText)
        }
        
        // initialize max and progress
        seekbarSize.max = 100
        seekbarSize.progress = ((subSizeScale * 100) - 50).toInt()
        updateSizeUI()

        seekbarSize.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) {
                    subSizeScale = (progress + 50) / 100f
                    updateSizeUI()
                    applyCaptionStyle()
                }
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })
        
        fun setSize(scale: Float) {
            subSizeScale = scale
            updateSizeUI()
            applyCaptionStyle()
        }
        btnSize75.setOnClickListener { setSize(0.75f) }
        btnSize100.setOnClickListener { setSize(1.0f) }
        btnSize125.setOnClickListener { setSize(1.25f) }
        btnSize150.setOnClickListener { setSize(1.5f) }

        // Subtitle Colors
        val containerWhite = view.findViewById<View>(R.id.color_white_container)
        val containerYellow = view.findViewById<View>(R.id.color_yellow_container)
        val containerCyan = view.findViewById<View>(R.id.color_cyan_container)
        val containerGreen = view.findViewById<View>(R.id.color_green_container)

        fun updateColorUI() {
            containerWhite.setBackgroundResource(if (subColor == Color.WHITE) R.drawable.bg_color_selected else android.R.color.transparent)
            containerYellow.setBackgroundResource(if (subColor == Color.YELLOW) R.drawable.bg_color_selected else android.R.color.transparent)
            containerCyan.setBackgroundResource(if (subColor == Color.CYAN) R.drawable.bg_color_selected else android.R.color.transparent)
            containerGreen.setBackgroundResource(if (subColor == Color.GREEN) R.drawable.bg_color_selected else android.R.color.transparent)
        }
        updateColorUI()

        fun setColor(color: Int) {
            subColor = color
            updateColorUI()
            applyCaptionStyle()
        }
        view.findViewById<View>(R.id.color_white).setOnClickListener { setColor(Color.WHITE) }
        view.findViewById<View>(R.id.color_yellow).setOnClickListener { setColor(Color.YELLOW) }
        view.findViewById<View>(R.id.color_cyan).setOnClickListener { setColor(Color.CYAN) }
        view.findViewById<View>(R.id.color_green).setOnClickListener { setColor(Color.GREEN) }

        // Edge Style
        val btnEdgeOutline = view.findViewById<Button>(R.id.btn_edge_outline)
        val btnEdgeShadow = view.findViewById<Button>(R.id.btn_edge_shadow)

        fun updateEdgeUI() {
            val dark = R.drawable.bg_btn_dark
            val purple = R.drawable.bg_btn_purple
            val whiteText = Color.WHITE
            val grayText = Color.parseColor("#888899")

            btnEdgeOutline.setBackgroundResource(if (isEdgeOutline) purple else dark)
            btnEdgeOutline.setTextColor(if (isEdgeOutline) whiteText else grayText)
            
            btnEdgeShadow.setBackgroundResource(if (isEdgeShadow) purple else dark)
            btnEdgeShadow.setTextColor(if (isEdgeShadow) whiteText else grayText)
        }
        updateEdgeUI()

        btnEdgeOutline.setOnClickListener {
            if (isEdgeOutline) {
                isEdgeOutline = false
            } else {
                isEdgeOutline = true
                isEdgeShadow = false
            }
            updateEdgeUI()
            applyCaptionStyle()
        }
        btnEdgeShadow.setOnClickListener {
            if (isEdgeShadow) {
                isEdgeShadow = false
            } else {
                isEdgeShadow = true
                isEdgeOutline = false
            }
            updateEdgeUI()
            applyCaptionStyle()
        }

        // Quality Settings
        val spinnerQuality = view.findViewById<Spinner>(R.id.spinner_quality)
        val qualityList = mutableListOf("Auto")
        val heights = mutableListOf<Int>()
        
        player?.currentTracks?.groups?.forEach { group ->
            if (group.type == androidx.media3.common.C.TRACK_TYPE_VIDEO) {
                for (i in 0 until group.length) {
                    val format = group.getTrackFormat(i)
                    if (format.height > 0 && !heights.contains(format.height)) {
                        qualityList.add("${format.height}p")
                        heights.add(format.height)
                    }
                }
            }
        }
        val qAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, qualityList)
        qAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerQuality.adapter = qAdapter

        // Check current selection
        val currentOverride = player?.trackSelectionParameters?.overrides?.values?.firstOrNull()
        if (currentOverride == null) {
            spinnerQuality.setSelection(0) // Auto
        } else {
            // Very basic matching for demonstration
            val currentHeight = currentOverride.trackIndices.firstOrNull()?.let { 
                // We'd map this properly, but for simplicity, let's just keep Auto as default
            }
        }

        spinnerQuality.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(p0: AdapterView<*>?, p1: View?, position: Int, p3: Long) {
                if (position == 0) {
                    // Auto
                    player?.trackSelectionParameters = player!!.trackSelectionParameters
                        .buildUpon()
                        .clearOverridesOfType(androidx.media3.common.C.TRACK_TYPE_VIDEO)
                        .build()
                } else {
                    val targetHeight = heights[position - 1]
                    player?.currentTracks?.groups?.forEach { group ->
                        if (group.type == androidx.media3.common.C.TRACK_TYPE_VIDEO) {
                            for (i in 0 until group.length) {
                                if (group.getTrackFormat(i).height == targetHeight) {
                                    val override = androidx.media3.common.TrackSelectionOverride(
                                        group.mediaTrackGroup, listOf(i)
                                    )
                                    player?.trackSelectionParameters = player!!.trackSelectionParameters
                                        .buildUpon()
                                        .setOverrideForType(override)
                                        .build()
                                    return
                                }
                            }
                        }
                    }
                }
            }
            override fun onNothingSelected(p0: AdapterView<*>?) {}
        }

        dialog.show()
    }


    // ── ViewModel observation ─────────────────────────────────────────────────

    private fun setupViewModel() {
        viewModel = ViewModelProvider(this, StreamViewModel.Factory())[StreamViewModel::class.java]

        viewModel.uiState.observe(this) { state ->
            if (state.episodes.isNotEmpty()) episodeAdapter.submitList(state.episodes)
            if (state.episodeRanges.isNotEmpty()) setupRangeDropdown(state.episodeRanges)

            setupServerControls(state)

            state.activeM3u8Url?.let { loadOrUpdatePlayer(it, state.activeTracks) }

            state.currentEpNumber?.let { ep ->
                val title = state.animeName ?: "Streaming"
                val tvDesc = findViewById<TextView>(R.id.tv_watching_desc)
                tvDesc.text = "Episode $ep"
                updateStreamTitle(title)
            }

            state.errorMessage?.let {
                Toast.makeText(this, it, Toast.LENGTH_SHORT).show()
            }
        }

        viewModel.loadEpisode(
            slug = animeSlug!!,
            epNumber = currentEpNumber!!,
            animeName = intent.getStringExtra("EXTRA_ANIME_NAME")
        )
    }

    private fun setupServerControls(state: StreamUiState) {
        val hasDub = state.dubSources.isNotEmpty()
        val subView = findViewById<View>(R.id.dropdown_sub)
        val dubView = findViewById<View>(R.id.dropdown_softsub)
        val hiddenDub = findViewById<View>(R.id.dropdown_dub)
        hiddenDub.visibility = View.GONE

        // SUB button
        if (state.subSources.isNotEmpty()) {
            subView.visibility = View.VISIBLE
            val tvSub = subView.findViewById<TextView>(R.id.tv_label)
            tvSub.text = if (activeCategory == "sub")
                "▶ SUB: ${state.subSources.getOrNull(subServerIdx)?.server ?: "Server"}"
            else "SUB"
            subView.setOnClickListener {
                if (activeCategory != "sub") {
                    activeCategory = "sub"
                    setupServerControls(state)
                    applySelectedSource(state)
                } else {
                    showServerPicker("SUB", state.subSources, subServerIdx) { idx ->
                        subServerIdx = idx
                        setupServerControls(state)
                        applySelectedSource(state)
                    }
                }
            }
        } else {
            subView.visibility = View.GONE
        }

        // DUB button
        if (hasDub) {
            dubView.visibility = View.VISIBLE
            val tvDub = dubView.findViewById<TextView>(R.id.tv_label)
            tvDub.text = if (activeCategory == "dub")
                "▶ DUB: ${state.dubSources.getOrNull(dubServerIdx)?.server ?: "Server"}"
            else "DUB"
            dubView.setOnClickListener {
                if (activeCategory != "dub") {
                    activeCategory = "dub"
                    setupServerControls(state)
                    applySelectedSource(state)
                } else {
                    showServerPicker("DUB", state.dubSources, dubServerIdx) { idx ->
                        dubServerIdx = idx
                        setupServerControls(state)
                        applySelectedSource(state)
                    }
                }
            }
        } else {
            dubView.visibility = View.GONE
        }
    }

    private fun showServerPicker(
        type: String,
        sources: List<SourceItem>,
        currentIdx: Int,
        onPick: (Int) -> Unit
    ) {
        val names = sources.mapIndexed { i, s ->
            val mark = if (i == currentIdx) "✓ " else "   "
            "$mark${s.server ?: "Server ${i + 1}"}"
        }.toTypedArray()
        AlertDialog.Builder(this)
            .setTitle("Select $type Server")
            .setItems(names) { _, which -> onPick(which) }
            .show()
    }

    private fun applySelectedSource(state: StreamUiState) {
        val source = if (activeCategory == "sub")
            state.subSources.getOrNull(subServerIdx)
        else
            state.dubSources.getOrNull(dubServerIdx)
        source?.let { viewModel.selectSource(it) }
    }

    override fun onDestroy() {
        player?.release()
        super.onDestroy()
    }
}
