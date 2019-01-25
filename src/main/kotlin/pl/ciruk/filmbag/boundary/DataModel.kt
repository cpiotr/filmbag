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

data class ScoreRequest(
        val grade: Double? = null,
        val quantity: Long? = null,
        val url: String? = null) {
    fun isValid(): Boolean {
        return grade != null && quantity != null
    }
}

sealed class Range<in T : Comparable<T>>
class LeftClosedRange<T : Comparable<T>>(val from: T) : Range<T>()
class RightClosedRange<T : Comparable<T>>(val to: T) : Range<T>()
class ClosedRange<T : Comparable<T>>(val from: T, val to: T) : Range<T>()
class EmptyRange<T : Comparable<T>> : Range<T>()
