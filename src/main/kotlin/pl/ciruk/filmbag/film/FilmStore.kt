package pl.ciruk.filmbag.film

import io.github.oshai.kotlinlogging.KotlinLogging
import org.hibernate.exception.ConstraintViolationException
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.stereotype.Service
import org.springframework.transaction.support.TransactionTemplate

@Service
class FilmStore(
    private val genreService: GenreService,
    private val filmService: FilmService,
    private val transactionTemplate: TransactionTemplate
) {
    private val logger = KotlinLogging.logger {}

    fun storeAll(films: List<FilmData>) {
        val (validFilms, invalidFilms) = films.partition { it.missingRequiredFields().isEmpty() }
        invalidFilms.forEach {
            logger.warn { "Skipping film <${it.title}> missing ${it.missingRequiredFields().joinToString()}" }
        }

        try {
            transactionTemplate.executeWithoutResult { store(validFilms) }
        } catch (e: DataIntegrityViolationException) {
            if (!e.isUniqueViolation()) throw e
            // Another request stored one of these films or genres after this one looked for them.
            // A new transaction finds them and updates them instead.
            logger.info { "Storing again after a concurrent insert: ${e.message}" }
            transactionTemplate.executeWithoutResult { store(validFilms) }
        }
    }

    private fun DataIntegrityViolationException.isUniqueViolation() =
        (cause as? ConstraintViolationException)?.kind == ConstraintViolationException.ConstraintKind.UNIQUE

    private fun store(validFilms: List<FilmData>) {
        val genresByName = genreService.merge(validFilms.flatMap { it.genres }).associateBy { it.name }

        filmService.storeAll(validFilms.map { convertToFilm(it, genresByName) })
    }

    private fun convertToFilm(data: FilmData, genresByName: Map<String, Genre>): Film {
        val genres = data.genres.map { genresByName.getValue(it) }.toSet()

        val film = Film(
                created = data.created,
                title = data.title!!,
                year = data.year!!,
                link = data.link!!,
                score = data.score!!,
                genres = genres,
                plot = trimToLimit(data.plot, PLOT_MAX_LENGTH),
                poster = data.poster
        )
        data.scores
                .filter(ScoreData::isValid)
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
