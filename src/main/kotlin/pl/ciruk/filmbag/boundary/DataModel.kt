package pl.ciruk.filmbag.boundary

import com.fasterxml.jackson.annotation.JsonIgnore
import pl.ciruk.filmbag.film.Film
import pl.ciruk.filmbag.film.ScoreType
import java.time.ZonedDateTime
import java.time.temporal.ChronoUnit

data class FilmRequest(
    val created: ZonedDateTime = ZonedDateTime.now().withFixedOffsetZone().truncatedTo(ChronoUnit.MICROS),
    val title: String? = null,
    val year: Int? = null,
    val plot: String? = null,
    val link: String? = null,
    val poster: String? = null,
    val score: Double? = null,
    val numberOfScores: Int = 0,
    val scores: Set<ScoreRequest> = mutableSetOf(),
    val genres: Set<String> = mutableSetOf()
) {
    fun missingRequiredFields(): List<String> = listOfNotNull(
        "title".takeIf { title == null },
        "year".takeIf { year == null },
        "link".takeIf { link == null },
        "score".takeIf { score == null }
    )
}

data class ScoreRequest(
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

fun Film.convertToRequest(): FilmRequest {
    return FilmRequest(
        created = this.created,
        title = this.title,
        year = this.year,
        link = this.link,
        score = this.score,
        numberOfScores = this.scores.size,
        scores = this.scores.map { ScoreRequest(it.grade, it.quantity, it.type.name, it.url) }.toSet(),
        genres = this.genres.map { it.name }.toSet(),
        plot = this.plot,
        poster = this.poster
    )
}
