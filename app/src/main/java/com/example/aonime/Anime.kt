package com.example.aonime

/**
 * Data class for an anime entry.
 * Ready to be populated from API response by backend team.
 *
 * @param id         Unique identifier (from API)
 * @param title      Anime title
 * @param type       Format type: "TV", "Movie", "ONA", "OVA"
 * @param episodes   Total episode count (0 = unknown/ongoing)
 * @param rating     Score out of 10 (empty if unrated)
 * @param posterUrl  Remote image URL (empty until API is connected)
 */
data class Anime(
    val id: Int,
    val title: String,
    val type: String,
    val episodes: Int,
    val rating: String,
    val posterUrl: String = ""
)
