package com.example.aonime

/**
 * Data class for an anime entry.
 *
 * @param id         Unique identifier (from API)
 * @param title      Anime title
 * @param type       Format type: "TV", "Movie", "ONA", "OVA"
 * @param episodes   Total episode count (0 = unknown/ongoing)
 * @param rating     Score or label shown in the card
 * @param posterUrl  Remote image URL
 */
data class Anime(
    val id: String,
    val title: String,
    val type: String,
    val episodes: Int,
    val rating: String,
    val posterUrl: String = ""
)
