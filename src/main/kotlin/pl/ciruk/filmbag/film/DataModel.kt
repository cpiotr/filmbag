package pl.ciruk.filmbag.film

import pl.ciruk.filmbag.boundary.FilmRequest
import pl.ciruk.filmbag.boundary.ScoreRequest
import java.util.*
import javax.persistence.*

@Entity
data class Film(
        @Id @GeneratedValue val id: Long? = null,
        val title: String,
        val year: Int,
        val plot: String? = null,
        val link: String,
        val poster: String? = null,
        val score: Double,
        @OneToMany(mappedBy = "film") val scores: Set<Score> = setOf(),
        @ManyToMany val genres: Set<Genre> = setOf(),
        val hash: Int = Objects.hash(title, year, genres))

@Entity
data class Score (
        @Id @GeneratedValue val id: Long? = null,
        val grade: Double,
        val quantity: Long,
        @ManyToOne @JoinColumn(name ="film_id") val film: Film? = null)

@Entity
data class Genre (
        @Id @GeneratedValue val id: Long? = null,
        val name: String)

fun FilmRequest.toFilm(): Film {
    val scores = this.scores.map { Score(grade = it.grade, quantity = it.quantity) }.toSet()
    val genres = this.genres.map { Genre(name = it) }.toSet()
    return Film(
            title = this.title,
            year = this.year,
            link = this.link,
            score = this.score,
            scores = scores,
            genres = genres,
            plot = this.plot,
            poster = this.poster

    )
}

fun Film.toRequest(): FilmRequest {
    return FilmRequest(
            title = this.title,
            year = this.year,
            link = this.link,
            score = this.score,
            numberOfScores = this.scores.size,
            scores = this.scores.map { ScoreRequest(it.grade, it.quantity) }.toList(),
            genres = this.genres.map { it.name }.toList(),
            plot = this.plot,
            poster = this.poster
    )
}
