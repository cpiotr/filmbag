package pl.ciruk.filmbag.film

import mu.KotlinLogging
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.data.jpa.domain.Specification
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.transaction.support.TransactionTemplate
import java.math.BigDecimal
import java.util.*
import jakarta.annotation.PostConstruct

@Service
class FilmService(
        private val repository: FilmRepository,
        private val transactionTemplate: TransactionTemplate
) {
    private val logger = KotlinLogging.logger {}

    @Transactional
    fun storeAll(films: List<Film>) {
        if (films.isEmpty()) {
            return
        }

        val hashes = films.map { it.identityHash }
        val storedFilmsByHash = repository
                .findAll(Specification { film, _, _ -> film.get<String>("identityHash").`in`(hashes) })
                .associateBy { it.identityHash }
        val (filmsToBeUpdated, filmsToBeStored) = films
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

    // @Transactional doesn't apply to @PostConstruct methods, hence the explicit transaction
    @PostConstruct
    private fun recalculateIdentityAndRemoveDuplicates() {
        transactionTemplate.executeWithoutResult {
            val (filmsToBeUpdated, filmsToBeDeleted) = selectUpdatableAndDeletable(repository.findAll())
            if (filmsToBeUpdated.isNotEmpty()) {
                repository.saveAll(filmsToBeUpdated)
                logger.info { "Updated ${filmsToBeUpdated.size} films" }
            }

            if (filmsToBeDeleted.isNotEmpty()) {
                repository.deleteAll(filmsToBeDeleted)
                logger.info { "Deleted ${filmsToBeDeleted.size} films" }
            }
        }
    }

    private fun selectUpdatableAndDeletable(films: Collection<Film>): Pair<MutableList<Film>, MutableList<Film>> {
        val filmsToBeUpdated = mutableListOf<Film>()
        val filmsToBeDeleted = mutableListOf<Film>()

        val filmsByHash = films.mapNotNull { copyIfHashChanged(it) }
                .groupBy { it.identityHash }

        filmsByHash
                .values
                .map { it.sortedByDescending(Film::created) }
                .forEach {
                    if (it.size > 1) {
                        logger.info { "Films sharing the same hash: ${it.map(Film::toString)}" }
                    }
                    filmsToBeUpdated.add(it.first())
                    filmsToBeDeleted.addAll(it.drop(1))
                }
        return Pair(filmsToBeUpdated, filmsToBeDeleted)
    }

    private fun copyIfHashChanged(it: Film): Film? {
        val hash = identityHashOf(it.title, it.year, it.genres)
        return if (hash != it.identityHash) {
            it.copy(identityHash = hash)
        } else {
            null
        }
    }
}
