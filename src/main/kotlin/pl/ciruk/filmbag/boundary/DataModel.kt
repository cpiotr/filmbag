package pl.ciruk.filmbag.boundary

import java.time.ZonedDateTime

data class FilmRequest(
        val created: ZonedDateTime = ZonedDateTime.now().withFixedOffsetZone(),
        val title: String? = null,
        val year: Int? = null,
        val plot: String? = null,
        val link: String? = null,
        val poster: String? = null,
        val score: Double? = null,
        val numberOfScores: Int = 0,
        val scores: Set<ScoreRequest> = mutableSetOf(),
        val genres: Set<String> = mutableSetOf()
)

data class ScoreRequest (
        val grade: Double? = null,
        val quantity: Long? = null
)
