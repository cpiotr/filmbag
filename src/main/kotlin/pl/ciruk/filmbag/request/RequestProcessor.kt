package pl.ciruk.filmbag.request

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import pl.ciruk.filmbag.boundary.FilmRequest
import pl.ciruk.filmbag.boundary.ScoreRequest
import pl.ciruk.filmbag.film.Film
import pl.ciruk.filmbag.film.FilmService
import pl.ciruk.filmbag.film.GenreService
import pl.ciruk.filmbag.film.ScoreType

@Service
@Transactional
class RequestProcessor(private val genreService: GenreService, private val filmService: FilmService) {
    fun storeAll(filmRequests: List<FilmRequest>) {
        val genresFromRequests = filmRequests.flatMap { it.genres }.toSet()
        genreService.merge(genresFromRequests)

        val films = filmRequests.map { convertToFilm(it) }
        filmService.storeAll(films)
    }

    private fun convertToFilm(filmRequest: FilmRequest): Film {
        val genres = genreService.merge(filmRequest.genres)

        val film = Film(
                created = filmRequest.created,
                title = filmRequest.title!!,
                year = filmRequest.year!!,
                link = filmRequest.link!!,
                score = filmRequest.score!!,
                genres = genres,
                plot = filmRequest.plot,
                poster = filmRequest.poster
        )
        filmRequest.scores
                .filter(ScoreRequest::isValid)
                .forEach { film.addScore(it.grade!!, it.quantity!!, convertToScoreType(it.type), it.url!!) }
        return film
    }

    private fun convertToScoreType(type: String?): ScoreType {
        return when (type) {
            null -> ScoreType.UNKNOWN
            else -> ScoreType.valueOf(type)
        }
    }

}
