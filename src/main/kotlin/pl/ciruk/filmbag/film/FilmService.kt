package pl.ciruk.filmbag.film

import mu.KotlinLogging
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.data.jpa.domain.Specification
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal

@Service
class FilmService(private val repository: FilmRepository) {
    private val logger = KotlinLogging.logger {}

    @Transactional
    fun storeAll(films: List<Film>) {
        if (films.isEmpty()) {
            return
        }

        // A batch can name one film twice; the later copy's score wins and the scores of both are kept
        val distinctFilms = films
                .groupBy { it.identityHash }
                .values
                .map { sameFilms -> sameFilms.reduce { film, later -> film.also { copyScoreFrom(later).invoke(it) } } }
        val hashes = distinctFilms.map { it.identityHash }
        val storedFilmsByHash = repository
                .findAll(Specification { film, _, _ -> film.get<String>("identityHash").`in`(hashes) })
                .associateBy { it.identityHash }
        val (filmsToBeUpdated, filmsToBeStored) = distinctFilms
                .partition { storedFilmsByHash.containsKey(it.identityHash) }

        filmsToBeUpdated
                .map { storedFilmsByHash.getValue(it.identityHash) to it }
                .filter { (storedFilm, film) -> copyScoreFrom(film).invoke(storedFilm) }
                .map { (storedFilm, _) -> storedFilm }
                .apply {
                    val updated = repository.saveAll(this)
                    logger.info { "Updated ${updated.size} films" }
                }

        repository.saveAll(filmsToBeStored)
                .apply { logger.info { "Stored ${this.size} films" } }
    }

    @Transactional(readOnly = true)
    fun find(
            year: Range<Int> = Unbounded(),
            score: Range<BigDecimal> = Unbounded(),
            page: Int = 0,
            pageSize: Int = 10): List<Film> {
        logger.info { "Find by year=$year; score=$score; page=$page; pageSize=$pageSize" }

        val specification = Specification.allOf(listOfNotNull(
                createSpecification(year, "year"),
                createSpecification(score, "score")))
        val pageRequest = PageRequest.of(page, pageSize, Sort.by("created").descending())

        return repository
                .findAll(specification, pageRequest)
                .content
    }

    private fun <T : Comparable<T>> createSpecification(range: Range<T>, propertyName: String): Specification<Film>? {
        return when (range) {
            is AtLeast -> Specification { film, _, builder -> builder.greaterThanOrEqualTo(film[propertyName], range.from) }
            is AtMost -> Specification { film, _, builder -> builder.lessThanOrEqualTo(film[propertyName], range.to) }
            is Between -> Specification { film, _, builder -> builder.between(film[propertyName], range.from, range.to) }
            is Unbounded -> null
        }
    }

    private fun copyScoreFrom(film: Film): (Film) -> Boolean {
        return {
            val scoreChanged = if (it.score != film.score) {
                it.score = film.score
                true
            } else false
            val scoreListModified = film.scores.map { score -> it.addScore(score.grade, score.quantity, score.type, score.url) }
                    .fold(false, Boolean::or)
            scoreChanged.or(scoreListModified)
        }
    }
}
