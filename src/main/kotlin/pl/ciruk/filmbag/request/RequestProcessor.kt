package pl.ciruk.filmbag.request

import io.github.oshai.kotlinlogging.KotlinLogging
import org.hibernate.exception.ConstraintViolationException
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.stereotype.Service
import org.springframework.transaction.support.TransactionTemplate
import pl.ciruk.filmbag.boundary.FilmRequest
import pl.ciruk.filmbag.boundary.ScoreRequest
import pl.ciruk.filmbag.film.Film
import pl.ciruk.filmbag.film.FilmService
import pl.ciruk.filmbag.film.Genre
import pl.ciruk.filmbag.film.GenreService
import pl.ciruk.filmbag.film.ScoreType

@Service
class RequestProcessor(
    private val genreService: GenreService,
    private val filmService: FilmService,
    private val transactionTemplate: TransactionTemplate
) {
    private val logger = KotlinLogging.logger {}

    fun storeAll(filmRequests: List<FilmRequest>) {
        val (validRequests, invalidRequests) = filmRequests.partition { it.missingRequiredFields().isEmpty() }
        invalidRequests.forEach {
            logger.warn { "Skipping film <${it.title}> missing ${it.missingRequiredFields().joinToString()}" }
        }

        try {
            transactionTemplate.executeWithoutResult { store(validRequests) }
        } catch (e: DataIntegrityViolationException) {
            if (!e.isUniqueViolation()) throw e
            // Another request stored one of these films or genres after this one looked for them.
            // A new transaction finds them and updates them instead.
            logger.info { "Storing again after a concurrent insert: ${e.message}" }
            transactionTemplate.executeWithoutResult { store(validRequests) }
        }
    }

    private fun DataIntegrityViolationException.isUniqueViolation() =
        (cause as? ConstraintViolationException)?.kind == ConstraintViolationException.ConstraintKind.UNIQUE

    private fun store(validRequests: List<FilmRequest>) {
        val genresByName = genreService.merge(validRequests.flatMap { it.genres }).associateBy { it.name }

        val films = validRequests.map { convertToFilm(it, genresByName) }
        filmService.storeAll(films)
    }

    private fun convertToFilm(filmRequest: FilmRequest, genresByName: Map<String, Genre>): Film {
        val genres = filmRequest.genres.map { genresByName.getValue(it) }.toSet()

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
