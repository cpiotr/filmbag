package pl.ciruk.filmbag

data class Film(
        val title: String,
        val year: Int,
        val plot: String? = null,
        val link: String,
        val poster: String? = null,
        val score: Double,
        val numberOfScores: Int = 0,
        val scores: List<Score> = listOf(),
        val genres: List<String> = listOf())

data class Score (val grade: Double, val quantity: Long)
