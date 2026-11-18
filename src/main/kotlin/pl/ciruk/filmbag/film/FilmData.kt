package pl.ciruk.filmbag.film

import com.fasterxml.jackson.annotation.JsonIgnore
import java.time.ZonedDateTime
import java.time.temporal.ChronoUnit

data class FilmData(
    val created: ZonedDateTime = ZonedDateTime.now().withFixedOffsetZone().truncatedTo(ChronoUnit.MICROS),
    val title: String? = null,
    val year: Int? = null,
    val plot: String? = null,
    val link: String? = null,
    val poster: String? = null,
    val score: Double? = null,
    val numberOfScores: Int = 0,
    val scores: Set<ScoreData> = mutableSetOf(),
    val genres: Set<String> = mutableSetOf()
) {
    /** Why this film cannot be stored: missing fields, or values that don't fit their columns. Empty if it can. */
    fun problems(): List<String> = listOfNotNull(
        "title is missing".takeIf { title == null },
        "year is missing".takeIf { year == null },
        "link is missing".takeIf { link == null },
        "score is missing".takeIf { score == null },
        tooLong("title", title, 255),
        tooLong("link", link, 512),
        tooLong("poster", poster, 512),
        outOfRange("score", score)
    ) + genres.mapNotNull { tooLong("genre", it, 255) } +
        scores.flatMap { listOfNotNull(tooLong("score url", it.url, 512), outOfRange("score grade", it.grade)) }
}

private fun tooLong(name: String, value: String?, maxLength: Int) =
    "$name is longer than $maxLength characters".takeIf { value != null && value.length > maxLength }

// Scores are stored as DECIMAL(24,18), which holds six digits before the decimal point
private fun outOfRange(name: String, value: Double?) =
    "$name must be between -999999 and 999999".takeIf { value != null && !(value > -1_000_000 && value < 1_000_000) }

data class ScoreData(
    val grade: Double? = null,
    val quantity: Long? = null,
    val type: String = ScoreType.UNKNOWN.name,
    val url: String? = null
) {
    @JsonIgnore
    fun isValid(): Boolean {
        return grade != null && quantity != null
    }
}

fun Film.toFilmData(): FilmData {
    return FilmData(
        created = this.created,
        title = this.title,
        year = this.year,
        link = this.link,
        score = this.score,
        numberOfScores = this.scores.size,
        scores = this.scores.map { ScoreData(it.grade, it.quantity, it.type.name, it.url) }.toSet(),
        genres = this.genres.map { it.name }.toSet(),
        plot = this.plot,
        poster = this.poster
    )
}
