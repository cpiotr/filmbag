package pl.ciruk.filmbag.boundary

import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.server.ResponseStatusException
import org.springframework.web.servlet.support.ServletUriComponentsBuilder
import pl.ciruk.filmbag.film.FilmData
import pl.ciruk.filmbag.film.FilmService
import pl.ciruk.filmbag.film.FilmStore
import pl.ciruk.filmbag.film.rangeOf
import pl.ciruk.filmbag.film.toFilmData
import pl.ciruk.filmbag.journal.Journal
import java.math.BigDecimal

@RestController
@RequestMapping("/resources/films")
class FilmController(
    private val filmService: FilmService,
    private val filmStore: FilmStore,
    private val journal: Journal
) {
    private val logger = KotlinLogging.logger {}

    /** Films newest first, optionally filtered by year and score. */
    @GetMapping
    fun find(
        @RequestParam yearFrom: Int?,
        @RequestParam yearTo: Int?,
        @RequestParam scoreFrom: BigDecimal?,
        @RequestParam scoreTo: BigDecimal?,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "10") pageSize: Int
    ): FilmPage {
        if (page < 0 || pageSize !in 1..MAX_PAGE_SIZE) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "page must be at least 0 and pageSize between 1 and $MAX_PAGE_SIZE")
        }

        val films = filmService.find(rangeOf(yearFrom, yearTo), rangeOf(scoreFrom, scoreTo), page, pageSize)
        val next = ServletUriComponentsBuilder.fromCurrentRequest()
            .replaceQueryParam("page", page + 1)
            .toUriString()
            .takeIf { films.hasNext() }
        return FilmPage(films.content.map { it.toFilmData() }, next)
    }

    /** Stores new films and updates the scores of known ones. Stores nothing if any film is invalid. */
    @PostMapping
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun store(@RequestBody films: List<FilmData>) {
        val invalidFilms = films.mapIndexedNotNull { index, film ->
            film.problems().takeIf { it.isNotEmpty() }?.let { InvalidFilm(index, it) }
        }
        if (invalidFilms.isNotEmpty()) {
            throw InvalidFilmsException(invalidFilms)
        }

        logger.info { "Store ${films.size} films" }
        filmStore.storeAll(films)
        journal.recordAsync(films)
    }

    companion object {
        const val MAX_PAGE_SIZE = 100
    }
}

/** [next] is the URL of the next page, or null on the last page. */
data class FilmPage(val items: List<FilmData>, val next: String?)

/** [index] is the film's position in the request. */
data class InvalidFilm(val index: Int, val problems: List<String>)

class InvalidFilmsException(val invalidFilms: List<InvalidFilm>) : RuntimeException("Invalid films: $invalidFilms")
