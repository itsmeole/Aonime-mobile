package com.example.aonime

/**
 * Offline fallback data for when the API is unavailable.
 */
object DummyData {

    val trendingAnime = listOf(
        Anime("1",  "One Piece",                          "TV",    1155, "9.0"),
        Anime("2",  "One-Punch Man Season 3",             "TV",    12,   "8.5"),
        Anime("3",  "Solo Leveling Season 2",             "TV",    13,   "8.8"),
        Anime("4",  "Jujutsu Kaisen",                     "TV",    24,   "8.7"),
        Anime("5",  "Demon Slayer: Swordsmith Village",   "TV",    11,   "9.1"),
        Anime("6",  "Chainsaw Man",                       "TV",    12,   "8.3"),
        Anime("7",  "Gachiakuta",                         "TV",    12,   "8.4"),
        Anime("8",  "Black Clover",                       "TV",    170,  "7.9"),
        Anime("9",  "Noble Reincarnation",                "TV",    1,    "7.8"),
        Anime("10", "Jack-of-All-Trades, Party of None",  "TV",    1,    "7.5"),
    )

    val latestEpisodes = listOf(
        Anime("11", "Naruto: Shippuden",                "TV",    500,  "8.6"),
        Anime("12", "Bleach",                           "TV",    366,  "8.5"),
        Anime("13", "My Hero Academia: Vigilantes S2",  "TV",    1,    "8.2"),
        Anime("14", "Golden Kamuy Final Season",        "TV",    1,    "8.8"),
        Anime("15", "A Record of Mortal's Journey S3",  "ONA",   104,  "7.9"),
        Anime("16", "Battle Through The Heavens S5",    "ONA",   180,  "7.5"),
        Anime("17", "Renegade Immortal",                "ONA",   122,  "7.6"),
        Anime("18", "Tales of Herding Gods",            "ONA",   64,   "7.4"),
    )

    val allAnime: List<Anime>
        get() = (trendingAnime + latestEpisodes + listOf(
            Anime("19", "A Wild Last Boss Appeared!",           "TV",    12,   "7.6"),
            Anime("20", "A Gatherer's Adventure in Isekai",     "TV",    12,   "7.5"),
            Anime("21", "May I Ask for One Final Thing?",       "TV",    12,   "8.0"),
            Anime("22", "Campfire Cooking S2",                  "TV",    12,   "7.8"),
            Anime("23", "Pokémon Horizons: The Series",         "TV",    122,  "7.2"),
            Anime("24", "Case Closed",                          "TV",    1187, "8.2"),
            Anime("25", "Super Dragon Ball Heroes",             "ONA",   20,   "6.8"),
            Anime("26", "Soul Land 2: Peerless Tang Sect",      "ONA",   134,  "7.3"),
            Anime("27", "Sentenced to Be a Hero",               "TV",    1,    "7.7"),
            Anime("28", "Kunon the Sorcerer Can See",           "TV",    12,   "7.9"),
        )).distinctBy { it.id }
}
