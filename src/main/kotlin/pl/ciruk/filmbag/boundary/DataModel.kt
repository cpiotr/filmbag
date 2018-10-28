package pl.ciruk.filmbag.boundary

data class FilmRequest(
        val title: String,
        val year: Int,
        val plot: String? = null,
        val link: String,
        val poster: String? = null,
        val score: Double,
        val numberOfScores: Int = 0,
        val scores: List<ScoreRequest> = listOf(),
        val genres: List<String> = listOf())

data class ScoreRequest (val grade: Double, val quantity: Long)
