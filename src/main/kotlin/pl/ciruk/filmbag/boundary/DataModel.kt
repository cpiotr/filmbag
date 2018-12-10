package pl.ciruk.filmbag.boundary

import java.time.ZonedDateTime

data class FilmRequest(
        val created: ZonedDateTime = ZonedDateTime.now().withFixedOffsetZone(),
        val title: String,
        val year: Int,
        val plot: String? = null,
        val link: String,
        val poster: String? = null,
        val score: Double,
        val numberOfScores: Int = 0,
        val scores: Set<ScoreRequest> = setOf(),
        val genres: Set<String> = setOf())

data class ScoreRequest (val grade: Double, val quantity: Long)
