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
import coil.load

/**
 * Adapter for displaying anime cards in a RecyclerView.
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
        private val tvEpisodes: TextView   = itemView.findViewById(R.id.tv_episodes)

        fun bind(anime: Anime) {
            tvTitle.text   = anime.title
            tvBadge.text   = anime.type
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

            ivPoster.load(anime.posterUrl) {
                crossfade(true)
                placeholder(R.drawable.bg_poster_placeholder)
                error(R.drawable.bg_poster_placeholder)
            }

            itemView.setOnClickListener { onItemClick(anime) }
        }
    }

    class AnimeDiffCallback : DiffUtil.ItemCallback<Anime>() {
        override fun areItemsTheSame(oldItem: Anime, newItem: Anime) = oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: Anime, newItem: Anime) = oldItem == newItem
    }
}
