package pl.ciruk.filmbag.film

import mu.KotlinLogging
import org.hibernate.Hibernate
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.ScrollPosition
import org.springframework.data.domain.Slice
import org.springframework.data.domain.Sort
import org.springframework.data.domain.Window
import org.springframework.data.jpa.domain.Specification
import org.springframework.stereotype.Service
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.annotation.Transactional
import org.springframework.transaction.support.TransactionTemplate
import java.math.BigDecimal

@Service
class FilmService(
        private val repository: FilmRepository,
        transactionManager: PlatformTransactionManager
) {
    private val logger = KotlinLogging.logger {}
    private val readOnlyTransaction = TransactionTemplate(transactionManager).apply { isReadOnly = true }
    // The id breaks ties, so pages neither repeat nor skip films created at the same time
    private val newestFirst = Sort.by(Sort.Order.desc("created"), Sort.Order.desc("id"))

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
        // A slice, unlike a page, needs no count query
        return repository
                .findBy<Film, Slice<Film>>(specification) { it.slice(PageRequest.of(page, pageSize, newestFirst)) }
                .content
                .withScoresAndGenres()
    }

    /** Every film, newest first, a page at a time. Each page is read in its own transaction. */
    fun findAllInPages(pageSize: Int): Sequence<List<Film>> = sequence {
        var position: ScrollPosition = ScrollPosition.keyset()
        do {
            val window = readOnlyTransaction.execute {
                repository
                        .findBy<Film, Window<Film>>(Specification.unrestricted()) { it.sortBy(newestFirst).limit(pageSize).scroll(position) }
                        .also { window -> window.content.withScoresAndGenres() }
            }
            if (!window.isEmpty) {
                yield(window.content)
                position = window.positionAt(window.size() - 1)
            }
        } while (window.hasNext())
    }

    // Callers use the films after the transaction has ended, so load scores and genres now.
    // Batch fetching loads them for a whole page in one query each.
    private fun List<Film>.withScoresAndGenres() = onEach {
        Hibernate.initialize(it.scores)
        Hibernate.initialize(it.genres)
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
