package pl.ciruk.filmbag.request

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import pl.ciruk.filmbag.boundary.FilmRequest
import pl.ciruk.filmbag.film.Film
import pl.ciruk.filmbag.film.FilmService
import pl.ciruk.filmbag.film.GenreService

@Service
@Transactional
class RequestProcessor(private val genreService: GenreService, private val filmService: FilmService) {
    fun store(filmRequest: FilmRequest) {
        val film = convertToFilm(filmRequest)
        filmService.store(film)
    }

    fun storeAll(filmRequests: List<FilmRequest>) {
        val genresFromRequests = filmRequests.flatMap { it.genres }
        genreService.merge(genresFromRequests)
        filmRequests.forEach { store(it)}
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
        filmRequest.scores.forEach { film.addScore(it.grade!!, it.quantity!!, it.url!!) }
        return film
    }
}
