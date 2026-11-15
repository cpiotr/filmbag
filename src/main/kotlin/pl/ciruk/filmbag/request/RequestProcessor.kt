package pl.ciruk.filmbag.request

import mu.KotlinLogging
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
    private val logger = KotlinLogging.logger {}

    fun storeAll(filmRequests: List<FilmRequest>) {
        val (validRequests, invalidRequests) = filmRequests.partition { it.missingRequiredFields().isEmpty() }
        invalidRequests.forEach {
            logger.warn { "Skipping film <${it.title}> missing ${it.missingRequiredFields().joinToString()}" }
        }

        val genresFromRequests = validRequests.flatMap { it.genres }.toSet()
        genreService.merge(genresFromRequests)

        val films = validRequests.map { convertToFilm(it) }
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
                plot = trimToLimit(filmRequest.plot, PLOT_MAX_LENGTH),
                poster = filmRequest.poster
        )
        filmRequest.scores
                .filter(ScoreRequest::isValid)
                .forEach { film.addScore(it.grade!!, it.quantity!!, convertToScoreType(it.type), it.url) }
        return film
    }

    private fun convertToScoreType(type: String): ScoreType {
        return ScoreType.entries.firstOrNull { it.name == type }
            ?: ScoreType.UNKNOWN.also { logger.warn { "Unknown score type <$type>, stored as ${ScoreType.UNKNOWN}" } }
    }

}

/** Length of the `film.plot` column (V008). */
private const val PLOT_MAX_LENGTH = 512
private const val ELLIPSIS = "..."

fun trimToLimit(text: String?, limit: Int): String? {
    require(limit >= ELLIPSIS.length) { "Limit has to be at least ${ELLIPSIS.length}" }

    if (text == null) {
        return null
    }

    return if (text.length > limit) {
        text.substring(0, limit - ELLIPSIS.length) + ELLIPSIS
    } else {
        text
    }
}
