package com.example.aonime

import android.content.res.ColorStateList
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView

/**
 * Adapter for displaying anime cards in a RecyclerView.
 *
 * Usage (Horizontal trending / Vertical list / Grid):
 *   adapter = AnimeAdapter { anime -> /* handle click - navigate to detail */ }
 *   recyclerView.adapter = adapter
 *   adapter.submitList(DummyData.trendingAnime)
 *
 * When API is ready: replace DummyData.xxx with actual API response list.
 *
 * @param onItemClick Lambda called with the clicked [Anime] object.
 */
class AnimeAdapter(
    private val isGrid: Boolean = false,
    private val onItemClick: (Anime) -> Unit
) : ListAdapter<Anime, AnimeAdapter.AnimeViewHolder>(AnimeDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AnimeViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_anime_card, parent, false)
            
        val lp = view.layoutParams as ViewGroup.MarginLayoutParams
        val density = parent.context.resources.displayMetrics.density
        
        if (isGrid) {
            lp.width = ViewGroup.LayoutParams.MATCH_PARENT
            lp.height = (250 * density).toInt()
            val m = (6 * density).toInt()
            lp.setMargins(m, m, m, m)
        } else {
            lp.width = (140 * density).toInt()
            lp.height = (220 * density).toInt()
            val mEnd = (12 * density).toInt()
            lp.setMargins(0, 0, mEnd, 0)
        }
        view.layoutParams = lp

        return AnimeViewHolder(view)
    }

    override fun onBindViewHolder(holder: AnimeViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class AnimeViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

        private val ivPoster: ImageView    = itemView.findViewById(R.id.iv_poster)
        private val tvTitle: TextView      = itemView.findViewById(R.id.tv_anime_title)
        private val tvBadge: TextView      = itemView.findViewById(R.id.tv_type_badge)
        private val tvRating: TextView     = itemView.findViewById(R.id.tv_rating)
        private val tvEpisodes: TextView   = itemView.findViewById(R.id.tv_episodes)

        fun bind(anime: Anime) {
            tvTitle.text   = anime.title
            tvBadge.text   = anime.type
            tvRating.text  = anime.rating
            tvEpisodes.text = if (anime.episodes > 0)
                itemView.context.getString(R.string.episodes_format, anime.episodes)
            else "? eps"

            // Set badge color based on type
            val badgeColor = when (anime.type) {
                "TV"    -> R.color.badge_tv
                "Movie" -> R.color.badge_movie
                "ONA"   -> R.color.badge_ona
                "OVA"   -> R.color.badge_ova
                else    -> R.color.badge_tv
            }
            tvBadge.backgroundTintList = ColorStateList.valueOf(
                ContextCompat.getColor(itemView.context, badgeColor)
            )

            // Poster: posterUrl is empty until API integrated.
            // When API is ready, use Glide / Picasso / Coil here:
            //   Glide.with(itemView).load(anime.posterUrl).into(ivPoster)
            // For now, placeholder background is handled by XML drawable.
            if (anime.posterUrl.isBlank()) {
                ivPoster.setImageDrawable(null)
                ivPoster.setBackgroundResource(R.drawable.bg_poster_placeholder)
            }

            itemView.setOnClickListener { onItemClick(anime) }
        }
    }

    /** DiffUtil for efficient list updates when API data arrives */
    class AnimeDiffCallback : DiffUtil.ItemCallback<Anime>() {
        override fun areItemsTheSame(oldItem: Anime, newItem: Anime) = oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: Anime, newItem: Anime) = oldItem == newItem
    }
}
