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
import javax.annotation.PostConstruct

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
        logger.info { "Find by year=$year; score=$score" }

        val allSpecifications = listOf(Pair(year, "year"), Pair(score, "score"))
                .mapNotNull { createSpecification(it.first, it.second) }

        val pageRequest = PageRequest.of(page, pageSize, Sort.by("create").descending())

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
    private fun load() {
        val films = repository.findAll()

        val filmsToBeUpdated = mutableListOf<Film>()
        val filmsToBeDeleted = mutableListOf<Film>()
        films.mapNotNull { copyIfHashChanged(it) }
                .groupBy { it.hash }
                .values
                .map { it.sortedByDescending { film -> film.created } }
                .forEach {
                    filmsToBeUpdated.add(it.first())
                    filmsToBeDeleted.addAll(it.drop(1))
                }

        if (filmsToBeUpdated.isNotEmpty()) {
            repository.saveAll(filmsToBeUpdated)
            logger.info { "Updated ${filmsToBeUpdated.size} films" }
        }

        if (filmsToBeDeleted.isNotEmpty()) {
            repository.deleteAll(filmsToBeDeleted)
            logger.info { "Deleted ${filmsToBeDeleted.size} films" }
        }

        repository.findAll()
                .forEach { existingFilmHashes[it.hash] = it.id!! }
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
