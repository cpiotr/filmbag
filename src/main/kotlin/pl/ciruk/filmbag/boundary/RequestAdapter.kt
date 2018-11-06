package pl.ciruk.filmbag.boundary

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import pl.ciruk.filmbag.film.*

@Service
@Transactional
class RequestAdapter(private val genreService: GenreService) {
    fun convertToFilm(filmRequest: FilmRequest): Film {
        val scores = filmRequest.scores.map { Score(grade = it.grade, quantity = it.quantity) }.toSet()
        val genres = genreService.merge(filmRequest.genres)
        return Film(
                title = filmRequest.title,
                year = filmRequest.year,
                link = filmRequest.link,
                score = filmRequest.score,
                scores = scores,
                genres = genres,
                plot = filmRequest.plot,
                poster = filmRequest.poster

        )
    }

    fun convertToRequest(film: Film): FilmRequest {
        return FilmRequest(
                title = film.title,
                year = film.year,
                link = film.link,
                score = film.score,
                numberOfScores = film.scores.size,
                scores = film.scores.map { ScoreRequest(it.grade, it.quantity) }.toList(),
                genres = film.genres.map { it.name }.toList(),
                plot = film.plot,
                poster = film.poster
        )
    }
}

