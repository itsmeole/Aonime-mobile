package com.example.aonime

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView

class EpisodeAdapter(
    private val onItemClick: (EpisodeApiItem) -> Unit
) : ListAdapter<EpisodeApiItem, EpisodeAdapter.EpisodeViewHolder>(EpisodeDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): EpisodeViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_episode, parent, false)
        return EpisodeViewHolder(view)
    }

    override fun onBindViewHolder(holder: EpisodeViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class EpisodeViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvNumber: TextView = itemView.findViewById(R.id.tv_episode_number)
        private val tvTitle: TextView = itemView.findViewById(R.id.tv_episode_title)
        private val tvSub: TextView = itemView.findViewById(R.id.tv_badge_sub)
        private val tvDub: TextView = itemView.findViewById(R.id.tv_badge_dub)

        fun bind(episode: EpisodeApiItem) {
            tvNumber.text = episode.number ?: "?"
            tvTitle.text = episode.title ?: "Episode ${episode.number}"
            
            tvSub.visibility = if (episode.hasSub) View.VISIBLE else View.GONE
            tvDub.visibility = if (episode.hasDub) View.VISIBLE else View.GONE

            itemView.setOnClickListener { onItemClick(episode) }
        }
    }

    class EpisodeDiffCallback : DiffUtil.ItemCallback<EpisodeApiItem>() {
        override fun areItemsTheSame(oldItem: EpisodeApiItem, newItem: EpisodeApiItem) = oldItem.slug == newItem.slug
        override fun areContentsTheSame(oldItem: EpisodeApiItem, newItem: EpisodeApiItem) = oldItem == newItem
    }
}