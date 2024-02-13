package pl.ciruk.filmbag.film

import mu.KotlinLogging
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.data.jpa.domain.Specification
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import pl.ciruk.filmbag.boundary.*
import pl.ciruk.filmbag.boundary.ClosedRange
import java.math.BigDecimal
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import jakarta.annotation.PostConstruct

@Service
class FilmService(private val repository: FilmRepository) {
    private val logger = KotlinLogging.logger {}

    private val existingFilmHashes = ConcurrentHashMap<Int, Long>()

    @Transactional
    fun storeAll(films: List<Film>) {
        val (filmsToBeUpdated, filmsToBeStored) = films
                .partition { existingFilmHashes.containsKey(it.hash) }

        val filmsToBeUpdatedById = filmsToBeUpdated.associateBy { existingFilmHashes[it.hash] }
        repository.findAllById(filmsToBeUpdatedById.keys)
                .filter {
                    val film = filmsToBeUpdatedById[it.id] ?: error("Missing film data for id: ${it.id}")
                    copyScoreFrom(film).invoke(it)
                }
                .apply {
                    val updated = repository.saveAll(this)
                    logger.info { "Updated ${updated.size} films" }
                }

        repository.saveAll(filmsToBeStored)
                .onEach { existingFilmHashes[it.hash] = it.id!! }
                .apply { logger.info { "Stored ${this.size} films" } }
    }

    @Transactional(readOnly = true)
    fun find(
            year: Range<Int> = EmptyRange(),
            score: Range<BigDecimal> = EmptyRange(),
            page: Int = 0,
            pageSize: Int = 10): List<Film> {
        logger.info { "Find by year=$year; score=$score; page=$page; pageSize=$pageSize" }

        val allSpecifications = listOf(Pair(year, "year"), Pair(score, "score"))
                .mapNotNull { createSpecification(it.first, it.second) }

        val pageRequest = PageRequest.of(page, pageSize, Sort.by("created").descending())

        return if (allSpecifications.isEmpty()) {
            repository
                    .findAll(pageRequest)
                    .content
        } else {
            val specification = allSpecifications.reduce { first, second -> first.and(second)!! }
            repository
                    .findAll(specification, pageRequest)
                    .content
        }
    }

    private fun <T : Comparable<T>> createSpecification(range: Range<T>, propertyName: String): Specification<Film>? {
        return when (range) {
            is LeftClosedRange -> Specification { film, _, builder -> builder.greaterThanOrEqualTo(film[propertyName], range.from) }
            is RightClosedRange -> Specification { film, _, builder -> builder.lessThanOrEqualTo(film[propertyName], range.to) }
            is ClosedRange -> Specification { film, _, builder -> builder.between(film[propertyName], range.from, range.to) }
            is EmptyRange -> null
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

    @PostConstruct
    @Transactional
    private fun updateHashIfComponentsChanged() {
        val filmsById = repository.findAll()
                .associateBy(Film::id)
                .toMutableMap()

        val (filmsToBeUpdated, filmsToBeDeleted) = selectUpdatableAndDeletable(filmsById.values)
        if (filmsToBeUpdated.isNotEmpty()) {
            repository.saveAll(filmsToBeUpdated)
            logger.info { "Updated ${filmsToBeUpdated.size} films" }
            filmsToBeUpdated.forEach { filmsById[it.id] = it }
        }

        if (filmsToBeDeleted.isNotEmpty()) {
            repository.deleteAll(filmsToBeDeleted)
            logger.info { "Deleted ${filmsToBeDeleted.size} films" }
            filmsToBeDeleted.forEach { filmsById.remove(it.id) }
        }

        filmsById.values
                .forEach { existingFilmHashes[it.hash] = it.id!! }
    }

    private fun selectUpdatableAndDeletable(films: Collection<Film>): Pair<MutableList<Film>, MutableList<Film>> {
        val filmsToBeUpdated = mutableListOf<Film>()
        val filmsToBeDeleted = mutableListOf<Film>()

        val filmsByHash = films.mapNotNull { copyIfHashChanged(it) }
                .groupBy { it.hash }

        filmsByHash
                .values
                .map { it.sortedByDescending(Film::created) }
                .forEach {
                    logger.info { "Films sharing the same hash: ${it.map(Film::toString)}" }
                    filmsToBeUpdated.add(it.first())
                    filmsToBeDeleted.addAll(it.drop(1))
                }
        return Pair(filmsToBeUpdated, filmsToBeDeleted)
    }

    private fun copyIfHashChanged(it: Film): Film? {
        val hash = Objects.hash(it.title, it.year, it.genres)
        return if (hash != it.hash) {
            it.copy(hash = hash)
        } else {
            null
        }
    }
}
