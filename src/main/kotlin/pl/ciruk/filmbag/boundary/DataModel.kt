package pl.ciruk.filmbag.boundary

import pl.ciruk.filmbag.film.Film
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

data class ScoreRequest(
        val grade: Double? = null,
        val quantity: Long? = null,
        val url: String? = null) {
    fun isValid(): Boolean {
        return grade != null && quantity != null
    }
}


fun Film.convertToRequest(): FilmRequest {
    return FilmRequest(
            created = this.created,
            title = this.title,
            year = this.year,
            link = this.link,
            score = this.score,
            numberOfScores = this.scores.size,
            scores = this.scores.map { ScoreRequest(it.grade, it.quantity, it.url) }.toSet(),
            genres = this.genres.map { it.name }.toSet(),
            plot = this.plot,
            poster = this.poster
    )
}

sealed class Range<in T : Comparable<T>>
class LeftClosedRange<T : Comparable<T>>(val from: T) : Range<T>()
class RightClosedRange<T : Comparable<T>>(val to: T) : Range<T>()
class ClosedRange<T : Comparable<T>>(val from: T, val to: T) : Range<T>()
class EmptyRange<T : Comparable<T>> : Range<T>()
