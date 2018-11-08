package pl.ciruk.filmbag.boundary

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import pl.ciruk.filmbag.film.Film
import pl.ciruk.filmbag.film.GenreService
import pl.ciruk.filmbag.film.Score

@Service
@Transactional
class RequestAdapter(private val genreService: GenreService) {
    fun convertToFilm(filmRequest: FilmRequest): Film {
        val genres = genreService.merge(filmRequest.genres)
        val film = Film(
                title = filmRequest.title,
                year = filmRequest.year,
                link = filmRequest.link,
                score = filmRequest.score,
                genres = genres,
                plot = filmRequest.plot,
                poster = filmRequest.poster
        )
        filmRequest.scores.forEach { film.addScore(it.grade, it.quantity) }
        return film
    }

    fun convertToRequest(film: Film): FilmRequest {
        return FilmRequest(
                title = film.title,
                year = film.year,
                link = film.link,
                score = film.score,
                numberOfScores = film.scores.size,
                scores = film.scores.map { ScoreRequest(it.grade, it.quantity) },
                genres = film.genres.map { it.name },
                plot = film.plot,
                poster = film.poster
        )
    }
}

